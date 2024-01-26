package com.microservices.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.microservices.dto.PatientDTO;

@Controller
public class ExternalDataController {

	@GetMapping("/afficher-patients")
	public String afficherPatients(Model model) {
		// URL du microservice gateway
		String urlMicroserviceGateway = "http://localhost:8081";

		// Utilisez RestTemplate pour récupérer les données du microservice patients.
		RestTemplate restTemplate = new RestTemplate();

		// Appel au microservice gateway pour obtenir les patients
		PatientDTO[] patients = restTemplate.getForObject(urlMicroserviceGateway + "/patients/all", PatientDTO[].class);

		// Ajoutez les données récupérées au modèle pour les afficher dans la vue Thymeleaf.
		model.addAttribute("patients", patients);

		// Retournez le nom de la vue Thymeleaf que vous souhaitez utiliser pour afficher les données.
		return "afficher-patients";
	}

	@GetMapping("/afficher-details/{patientId}")
	public String afficherDetailsPatient(@PathVariable Long patientId, Model model) {
		String urlMicroserviceGateway = "http://localhost:8081";
		RestTemplate restTemplate = new RestTemplate();
		try {
			PatientDTO patient = restTemplate.getForObject(urlMicroserviceGateway + "/patients/" + patientId, PatientDTO.class);
			model.addAttribute("patient", patient);
			return "afficher-details";
		} catch (HttpClientErrorException.NotFound notFoundException) {
			model.addAttribute("errorMessage", "Patient not found");
			model.addAttribute("status", HttpStatus.NOT_FOUND.value());
			return "error";
		}
	}
}
