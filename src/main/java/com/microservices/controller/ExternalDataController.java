package com.microservices.controller;

import java.util.Arrays;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.microservices.dto.PatientDTO;
import com.microservices.dto.MedecinNoteDTO;

@Controller
public class ExternalDataController {
	
	// URL du microservice gateway
	// private final String urlMicroserviceGateway = "http://172.17.0.3:8081";
	private final String urlMicroserviceGateway = "http://localhost:8081";
	
	@GetMapping("/afficher-patients")
	public String afficherPatients(Model model) {
		RestTemplate restTemplate = new RestTemplate();
		PatientDTO[] patients = restTemplate.getForObject(urlMicroserviceGateway + "/patients/all", PatientDTO[].class);
		model.addAttribute("patients", patients);
		return "afficher-patients";
	}

	@GetMapping("/afficher-details/{patientId}")
	public String afficherDetailsPatient(@PathVariable Long patientId, Model model) {
	    RestTemplate restTemplate = new RestTemplate();
	    try {
	        PatientDTO patient = restTemplate.getForObject(urlMicroserviceGateway + "/patients/" + patientId, PatientDTO.class);
	        model.addAttribute("patient", patient);
	        ResponseEntity<MedecinNoteDTO[]> responseEntity = restTemplate.getForEntity(urlMicroserviceGateway + "/medecin/notes/" + patientId, MedecinNoteDTO[].class);
	        MedecinNoteDTO[] medecinNotes = responseEntity.getBody();
	        model.addAttribute("medecinNotes", Arrays.asList(medecinNotes));
	        return "afficher-details";
	    } catch (HttpClientErrorException.NotFound notFoundException) {
	        model.addAttribute("errorMessage", "Patient not found");
	        model.addAttribute("status", HttpStatus.NOT_FOUND.value());
	        return "error";
	    }
	}
		
	@PostMapping("/ajouter-patient")
	public String ajouterPatient(@ModelAttribute("newPatient") PatientDTO newPatient, Model model) {
        try {
            String backendUrl = urlMicroserviceGateway + "/patients/add";

            ResponseEntity<PatientDTO> responseEntity = new RestTemplate().postForEntity(
                    backendUrl,
                    newPatient,
                    PatientDTO.class
            );

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                return "redirect:/afficher-patients";
            } else {
                model.addAttribute("errorMessage", "Failed to add a new patient");
                model.addAttribute("status", responseEntity.getStatusCodeValue());
                return "error";
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Internal Server Error");
            model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return "error";
        }
    }
	
	 @GetMapping("/modifier-adresse/{patientId}")
	 public String afficherFormulaireModifierAdresse(@PathVariable Long patientId, Model model) {
	     PatientDTO patient = new RestTemplate().getForObject(urlMicroserviceGateway + "/patients/" + patientId, PatientDTO.class);
	     model.addAttribute("patient", patient);
	     return "modifier-adresse";
	 }

	 @PostMapping("/modifier-adresse/{patientId}")
	    public String modifierAdressePatient(@PathVariable Long patientId, @RequestParam String nouvelleAdresse, Model model) {
	        try {
	            String backendUrl = urlMicroserviceGateway + "/patients/" + patientId + "/update-adresse?nouvelleAdresse=" + nouvelleAdresse;
	            HttpEntity<Void> requestEntity = new HttpEntity<>(null);
	            ResponseEntity<PatientDTO> responseEntity = new RestTemplate().exchange(
	                    backendUrl,
	                    HttpMethod.POST,
	                    requestEntity,
	                    PatientDTO.class
	            );

	            if (responseEntity.getStatusCode().is2xxSuccessful()) {
	                return "redirect:/afficher-details/" + patientId;
	            } else {
	                model.addAttribute("errorMessage", "Failed to update address");
	                model.addAttribute("status", responseEntity.getStatusCodeValue());
	                return "error";
	            }
	        } catch (Exception e) {
	            model.addAttribute("errorMessage", "Internal Server Error");
	            model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
	            return "error";
	        }
	    }
	
	 @GetMapping("/modifier-numero/{patientId}")
	 public String afficherFormulaireModifierNumero(@PathVariable Long patientId, Model model) {
	     PatientDTO patient = new RestTemplate().getForObject(urlMicroserviceGateway + "/patients/" + patientId, PatientDTO.class);
	     model.addAttribute("patient", patient);
	     return "modifier-numero";
	 }
	 
	@PostMapping("/modifier-numero/{patientId}")
    public String modifierNumeroPatient(@PathVariable Long patientId, @RequestParam String nouveauNumero, Model model) {
        try {
            String backendUrl = urlMicroserviceGateway + "/patients/" + patientId + "/update-numero?nouveauNumero=" + nouveauNumero;

            HttpEntity<Void> requestEntity = new HttpEntity<>(null);

            ResponseEntity<PatientDTO> responseEntity = new RestTemplate().exchange(
                    backendUrl,
                    HttpMethod.POST,
                    requestEntity,
                    PatientDTO.class
            );

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                return "redirect:/afficher-details/" + patientId;
            } else {
                model.addAttribute("errorMessage", "Failed to update phone number");
                model.addAttribute("status", responseEntity.getStatusCodeValue());
                return "error";
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Internal Server Error");
            model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return "error";
        }
    }
		
	@GetMapping("/ajouter-patient")
	public String afficherFormulaireAjoutPatient(Model model) {
	    model.addAttribute("newPatient", new PatientDTO());
	    return "ajouter-patient";
	}

	@PostMapping("/ajouter-note/{patientId}")
	public String ajouterNoteMedicale(@PathVariable Long patientId, @RequestParam String nouvelleNote, Model model) {
	    try {
	        RestTemplate restTemplate = new RestTemplate();
	        PatientDTO patient = restTemplate.getForObject(urlMicroserviceGateway + "/patients/" + patientId, PatientDTO.class);

	        if (patient == null) {
	            return "error";
	        }

	        MedecinNoteDTO nouvelleNoteMedicale = new MedecinNoteDTO();
	        nouvelleNoteMedicale.setPatId(patientId);
	        nouvelleNoteMedicale.setPatient(patient.getNom());
	        nouvelleNoteMedicale.setNote(nouvelleNote);

	        String backendUrl = urlMicroserviceGateway + "/medecin/notes/" + patientId;

	        ResponseEntity<Void> responseEntity = new RestTemplate().postForEntity(
	            backendUrl,
	            nouvelleNoteMedicale,
	            Void.class
	        );

	        if (responseEntity.getStatusCode().is2xxSuccessful()) {
	            return "redirect:/afficher-details/" + patientId;
	        } else {
	            model.addAttribute("errorMessage", "Failed to add medical note");
	            model.addAttribute("status", responseEntity.getStatusCodeValue());
	            return "error";
	        }
	    } catch (Exception e) {
	        model.addAttribute("errorMessage", "Internal Server Error");
	        model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
	        return "error";
	    }
	}

	@GetMapping("/ajouter-note/{patientId}")
	public String afficherFormulaireAjoutNoteMedicale(@PathVariable Long patientId, Model model) {
	    RestTemplate restTemplate = new RestTemplate();
	    PatientDTO patient = restTemplate.getForObject(urlMicroserviceGateway + "/patients/" + patientId, PatientDTO.class);
	    model.addAttribute("patient", patient);
	    return "ajouter-note";
	}
}
