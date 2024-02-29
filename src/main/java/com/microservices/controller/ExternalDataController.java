package com.microservices.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
import com.microservices.dto.PatientDTO;
import com.microservices.dto.MedecinNoteDTO;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
@Controller
public class ExternalDataController {

	// URL du microservice gateway
	// private final String urlMicroserviceGateway = "http://192.168.1.3:8081";

	private final String urlMicroserviceGateway = "http://localhost:8081";

	private String username;
	private String password;

	// Méthode de gestion des exceptions pour l'authentification

	@ExceptionHandler(ResponseStatusException.class)
	public ModelAndView handleUnauthorized(ServerWebExchange exchange, ResponseStatusException ex) {
		if (ex.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
			System.out.println("erreur");
			ModelAndView modelAndView = new ModelAndView("redirect:/login");
			modelAndView.addObject("error", ex.getReason());
			return modelAndView;
		}
		return null;
	}

	@GetMapping("/login")
	public String afficherPageLogin(Model model) {
		return "login";
	}

	@PostMapping("/login")
	public String login(@RequestParam String username, @RequestParam String password) {
		// Sauvegarder le nom d'utilisateur et le mot de passe
		this.username = username;
		this.password = password;

		// Redirection vers la page de liste des patients
		return "redirect:/afficher-patients";
	}

	@GetMapping("/afficher-patients")
	public String afficherPatients(Model model) {

		// Récupérer les détails du patient depuis le microservice via la gateway
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(username, password);
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<PatientDTO[]> response = restTemplate.exchange(urlMicroserviceGateway + "/patients/all",
				HttpMethod.GET, entity, PatientDTO[].class);

		// Vérifier si la réponse est autorisée
		if (response.getStatusCode().equals(HttpStatus.OK)) {
			// Récupérer le rôle de l'utilisateur
			boolean isOrganisateur = (username.equals("org") && password.equals("org"));
			// Ajouter le rôle de l'utilisateur au modèle
			model.addAttribute("isOrganisateur", isOrganisateur);

			// Ajouter la liste des patients au modèle
			List<PatientDTO> patients = Arrays.asList(response.getBody());
			model.addAttribute("patients", patients);
			// Retourner la vue Thymeleaf
			return "afficher-patients";
		} else {
			// Redirection vers la page de login en cas d'échec d'authentification
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access");
		}
	}

	@GetMapping("/afficher-details/{patientId}")
	public String afficherDetailsPatientWithRisk(@PathVariable Long patientId, Model model) {

		// Récupérer les détails du patient
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(username, password);
		HttpEntity<String> entity = new HttpEntity<>(headers);

		try {
			// Récupérer les détails du patient depuis le microservice via la gateway
			ResponseEntity<PatientDTO> patientResponse = restTemplate.exchange(
					urlMicroserviceGateway + "/patients/" + patientId, HttpMethod.GET, entity, PatientDTO.class);

			// Vérifier si la réponse est autorisée
			if (patientResponse.getStatusCode().equals(HttpStatus.OK)) {
				PatientDTO patient = patientResponse.getBody();

				// Récupérer les notes du médecin associées au patient
				ResponseEntity<MedecinNoteDTO[]> notesResponse = restTemplate.exchange(
						urlMicroserviceGateway + "/medecin/notes/" + patientId, HttpMethod.GET, entity,
						MedecinNoteDTO[].class);

				// Calculer le niveau de risque de diabète
				String diabetesRisk = restTemplate
						.exchange(urlMicroserviceGateway + "/diabetes-risk/patients/" + patientId, HttpMethod.GET,
								entity, String.class)
						.getBody();

				// Ajouter les informations au modèle
				model.addAttribute("patient", patient);
				model.addAttribute("medecinNotes", Arrays.asList(notesResponse.getBody()));
				model.addAttribute("diabetesRisk", diabetesRisk);
				
				boolean isOrganisateur = (username.equals("org") && password.equals("org"));

				model.addAttribute("isOrganisateur", isOrganisateur);

				return "afficher-details";
			} else {
				// Redirection vers la page d'erreur en cas d'accès non autorisé
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access");
			}
		} catch (HttpClientErrorException.NotFound notFoundException) {
			// Gérer l'exception si le patient n'est pas trouvé
			model.addAttribute("errorMessage", "Patient not found");
			model.addAttribute("status", HttpStatus.NOT_FOUND.value());
			return "error";
		}
	}
	
	@GetMapping("/ajouter-patient")
	public String afficherFormulaireAjoutPatient(Model model) {
		model.addAttribute("newPatient", new PatientDTO());
		return "ajouter-patient";
	}

	@PostMapping("/ajouter-patient")
	public String ajouterPatient(@ModelAttribute("newPatient") PatientDTO newPatient, Model model) {
		try {
			String backendUrl = urlMicroserviceGateway + "/patients/add";

			// Créer une entité Http avec l'objet newPatient dans le corps de la requête
			HttpHeaders headers = new HttpHeaders();
			headers.setBasicAuth(username, password);

			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<PatientDTO> entity = new HttpEntity<>(newPatient, headers);

			// Envoyer une requête POST avec les données du nouveau patient
			RestTemplate restTemplate = new RestTemplate();
			ResponseEntity<PatientDTO> responseEntity = restTemplate.exchange(backendUrl, HttpMethod.POST, entity,
					PatientDTO.class);

			if (responseEntity.getStatusCode().equals(HttpStatus.OK)) {
				// Rediriger vers la page d'affichage des patients en cas de succès
				return "redirect:/afficher-patients";
			} else {
				// Gérer les erreurs en cas d'échec de l'ajout du patient
				model.addAttribute("errorMessage", "Failed to add a new patient");
				model.addAttribute("status", responseEntity.getStatusCodeValue());
				return "error";
			}
		} catch (Exception e) {
			// Gérer les exceptions génériques en cas d'erreur interne du serveur
			model.addAttribute("errorMessage", "Internal Server Error");
			model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
			return "error";
		}
	}

	@GetMapping("/modifier-adresse/{patientId}")
	public String afficherFormulaireModifierAdresse(@PathVariable Long patientId, Model model) {
		// Récupérer les détails du patient
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(username, password);
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<PatientDTO> patientResponse = restTemplate
				.exchange(urlMicroserviceGateway + "/patients/" + patientId, HttpMethod.GET, entity, PatientDTO.class);
		model.addAttribute("patient", patientResponse.getBody());
		return "modifier-adresse";
	}

	@PostMapping("/modifier-adresse/{patientId}")
	public String modifierAdressePatient(@PathVariable Long patientId, @RequestParam String nouvelleAdresse,
			Model model) {
		try {
			// Créer une entité Http avec les données du corps de la requête
			HttpHeaders headers = new HttpHeaders();
			headers.setBasicAuth(username, password);
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(headers);
			RestTemplate restTemplate = new RestTemplate();
			// Récupérer les détails du patient
			ResponseEntity<PatientDTO> patientResponse = restTemplate.exchange(
					urlMicroserviceGateway + "/patients/" + patientId, HttpMethod.GET, entity, PatientDTO.class);

			String backendUrl = urlMicroserviceGateway + "/patients/" + patientId + "/update-adresse?nouvelleAdresse="
					+ nouvelleAdresse;

			// Envoyer la requête pour mettre à jour l'adresse du patient
			ResponseEntity<PatientDTO> responseEntity = restTemplate.exchange(backendUrl, HttpMethod.POST, entity,
					PatientDTO.class);

			if (responseEntity.getStatusCode().equals(HttpStatus.OK)) {
				// Rediriger vers la page d'affichage des patients en cas de succès
				return "redirect:/afficher-patients";
			} else {
				// Gérer les erreurs en cas d'échec de l'ajout du patient
				model.addAttribute("errorMessage", "Failed to add a new patient");
				model.addAttribute("status", responseEntity.getStatusCodeValue());
				return "error";
			}
		} catch (Exception e) {
			// Gérer les exceptions génériques en cas d'erreur interne du serveur
			model.addAttribute("errorMessage", "Internal Server Error");
			model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
			return "error";
		}
	}

	@GetMapping("/modifier-numero/{patientId}")
	public String afficherFormulaireModifierNumero(@PathVariable Long patientId, Model model) {
		// Récupérer les détails du patient
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(username, password);
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<PatientDTO> patientResponse = restTemplate
				.exchange(urlMicroserviceGateway + "/patients/" + patientId, HttpMethod.GET, entity, PatientDTO.class);
		model.addAttribute("patient", patientResponse.getBody());
		return "modifier-numero";
	}

	@PostMapping("/modifier-numero/{patientId}")
	public String modifierNumeroPatient(@PathVariable Long patientId, @RequestParam String nouveauNumero, Model model) {
		try {
			// Créer une entité Http avec les données du corps de la requête
			HttpHeaders headers = new HttpHeaders();
			headers.setBasicAuth(username, password);
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(headers);
			RestTemplate restTemplate = new RestTemplate();
			// Récupérer les détails du patient
			ResponseEntity<PatientDTO> patientResponse = restTemplate.exchange(
					urlMicroserviceGateway + "/patients/" + patientId, HttpMethod.GET, entity, PatientDTO.class);
			// Construire l'URL du backend pour la modification du numéro de téléphone du
			// patient

			String backendUrl = urlMicroserviceGateway + "/patients/" + patientId + "/update-numero?nouveauNumero="
					+ nouveauNumero;
			// Envoyer la requête pour mettre à jour l'adresse du patient
			ResponseEntity<PatientDTO> responseEntity = restTemplate.exchange(backendUrl, HttpMethod.POST, entity,
					PatientDTO.class);
			if (responseEntity.getStatusCode().equals(HttpStatus.OK)) {
				// Rediriger vers la page d'affichage des détails du patient en cas de succès
				return "redirect:/afficher-details/" + patientId;
			} else {
				// Gérer les erreurs en cas d'échec de la modification du numéro de téléphone
				model.addAttribute("errorMessage", "Failed to update phone number");
				model.addAttribute("status", responseEntity.getStatusCodeValue());
				return "error";
			}
		} catch (HttpClientErrorException.Unauthorized unauthorizedException) {
			// Intercepter l'exception d'authentification non autorisée
			model.addAttribute("errorMessage", "Unauthorized access. Please check your credentials.");
			model.addAttribute("status", HttpStatus.UNAUTHORIZED.value());
			return "error";
		} catch (Exception e) {
			// Gérer les exceptions génériques en cas d'erreur interne du serveur
			model.addAttribute("errorMessage", "Internal Server Error");
			model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
			return "error";
		}
	}

	@GetMapping("/ajouter-note/{patientId}")
	public String afficherFormulaireAjoutNoteMedicale(@PathVariable Long patientId, Model model) {
		// Récupérer les détails du patient
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(username, password);
		HttpEntity<String> entity = new HttpEntity<>(headers);
		ResponseEntity<PatientDTO> patientResponse = restTemplate
				.exchange(urlMicroserviceGateway + "/patients/" + patientId, HttpMethod.GET, entity, PatientDTO.class);
		model.addAttribute("patient", patientResponse.getBody());
		return "ajouter-note";
	}
	
	@PostMapping("/ajouter-note/{patientId}")
	public String ajouterNoteMedicale(@PathVariable Long patientId, @RequestParam String nouvelleNote, Model model) {
		try {
			// Récupérer les détails du patient
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.setBasicAuth(username, password);
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> entity = new HttpEntity<>(headers);

			// Récupérer les détails du patient depuis le microservice via la gateway
			ResponseEntity<PatientDTO> patientResponse = restTemplate.exchange(
					urlMicroserviceGateway + "/patients/" + patientId, HttpMethod.GET, entity, PatientDTO.class);
			PatientDTO patient = patientResponse.getBody();

			// Créer un objet représentant la nouvelle note médicale
			MedecinNoteDTO nouvelleNoteMedicale = new MedecinNoteDTO();
			nouvelleNoteMedicale.setPatId(patientId);
			nouvelleNoteMedicale.setPatient(patient.getNom());
			nouvelleNoteMedicale.setNote(nouvelleNote);

			// Construire l'URL du backend pour l'ajout de la note médicale
			//String backendUrl = urlMicroserviceGateway + "/medecin/notes";
			String backendUrl = urlMicroserviceGateway + "/medecin/notes/" + patientId;

			HttpEntity<MedecinNoteDTO> requestEntity = new HttpEntity<>(nouvelleNoteMedicale, headers);

			ResponseEntity<MedecinNoteDTO> responseEntity = restTemplate.exchange(backendUrl, HttpMethod.POST, requestEntity,
					MedecinNoteDTO.class);

			if (responseEntity.getStatusCode().equals(HttpStatus.OK)) {
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

	@GetMapping("/error")
	public String afficherPageErreur(Model model) {
		return "error";
	}
}
