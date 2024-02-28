package com.microservices.controller;

import java.util.Arrays;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.microservices.dto.PatientDTO;
import com.microservices.dto.MedecinNoteDTO;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
@Controller
public class ExternalDataController {
	
	// URL du microservice gateway
   // private final String urlMicroserviceGateway = "http://192.168.1.3:8081";
    
	private final String urlMicroserviceGateway = "http://localhost:8081";
	
    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView handleUnauthorized(ServerWebExchange exchange, ResponseStatusException ex) {
        if (ex.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) { 
            ModelAndView modelAndView = new ModelAndView("redirect:/login");
            modelAndView.addObject("error", ex.getReason());
            return modelAndView;
        }
        return null;
    }
	
	@GetMapping("/afficher-patients")
	public String afficherPatients(Model model) {

	    // Récupérer la liste des patients depuis votre microservice
	    RestTemplate restTemplate = new RestTemplate();
	    PatientDTO[] patients = restTemplate.getForObject(urlMicroserviceGateway + "/patients/all", PatientDTO[].class);

	    // Ajouter la liste des patients au modèle
	    model.addAttribute("patients", patients);

	    // Retourner la vue Thymeleaf
	    return "afficher-patients";
	}
	
	@GetMapping("/afficher-details/{patientId}")
	public String afficherDetailsPatientWithRisk(@PathVariable Long patientId, Model model) {
	    RestTemplate restTemplate = new RestTemplate();
	    try {
	        // Récupérer les détails du patient
	        PatientDTO patient = restTemplate.getForObject(urlMicroserviceGateway + "/patients/" + patientId, PatientDTO.class);
	        
	        // Récupérer les notes du médecin associées au patient
	        ResponseEntity<MedecinNoteDTO[]> responseEntity = restTemplate.getForEntity(urlMicroserviceGateway + "/medecin/notes/" + patientId, MedecinNoteDTO[].class);
	        MedecinNoteDTO[] medecinNotes = responseEntity.getBody();

	        // Calculer le niveau de risque de diabète
	        String diabetesRisk = restTemplate.getForObject(urlMicroserviceGateway + "/diabetes-risk/patients/" + patientId, String.class);
	        
	        // Ajouter les informations au modèle
	        model.addAttribute("patient", patient);
	        model.addAttribute("medecinNotes", Arrays.asList(medecinNotes));
	        model.addAttribute("diabetesRisk", diabetesRisk);
	       
	        
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
	
	@GetMapping("/login")
	public String afficherPageLogin(Model model) {
	    return "login";
	}
	
	 @PostMapping("/login")
	    public String authenticate(@RequestParam String username, @RequestParam String password, RedirectAttributes redirectAttributes) {
	        // Création d'un objet de demande contenant les informations d'identification
	        HttpHeaders headers = new HttpHeaders();
	        // Requête avec des paramètres encodés en URL
	        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

	        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
	        map.add("username", username);
	        map.add("password", password);
	        	       	        
	        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
	        
	        System.out.println("request "+request.getBody());
	        
	        // Appel de la gateway avec les informations d'authentification
	        ResponseEntity<String> response = new RestTemplate().postForEntity("http://localhost:8081/login", request, String.class);
	        System.out.println("badr2");

	        // Traitement de la réponse de la gateway
	        if (response.getStatusCode() == HttpStatus.OK) {
	            // Authentification réussie, rediriger vers la page souhaitée
	            return "redirect:/afficher-patients";
	        } else {
	            // Authentification échouée, retourner vers la page de connexion avec un message d'erreur
	            redirectAttributes.addFlashAttribute("error", "Identifiants invalides");
	            return "redirect:/login";
	        }
	    }

	@GetMapping("/error")
	public String afficherPageErreur(Model model) {
	    return "error";
	}
}
