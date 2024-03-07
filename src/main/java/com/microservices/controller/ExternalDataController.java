package com.microservices.controller;

import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.microservices.dto.PatientDTO;
import com.microservices.dto.MedecinNoteDTO;

@Controller
public class ExternalDataController {

	// URL du microservice gateway
	// private final String urlMicroserviceGateway = "http://192.168.1.3:8081";

	private final String urlMicroserviceGateway = "http://localhost:8081";

	private RestTemplate restTemplate;
	private String username;
	private String password;

	public ExternalDataController() {
		this.restTemplate = new RestTemplate();
	}

	// Crée une entité HTTP avec l'authentification basique

	private <T> HttpEntity<T> createHttpEntityWithBasicAuth(T body) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(username, password);
		return new HttpEntity<>(body, headers);
	}

	// Crée une entité HTTP avec l'authentification basique sans corps de requête

	private HttpEntity<String> createHttpEntityWithBasicAuth() {
		return createHttpEntityWithBasicAuth(null);
	}

	// Méthode de gestion des exceptions pour l'authentification
	@ExceptionHandler(HttpClientErrorException.Unauthorized.class)
	public String handleUnauthorized(HttpClientErrorException.Unauthorized ex) {
		if (ex.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
			return "redirect:/login";
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
	    HttpEntity<String> entity = createHttpEntityWithBasicAuth();
	    ResponseEntity<PatientDTO[]> response = restTemplate.exchange(
	        urlMicroserviceGateway + "/patients/all",
	        HttpMethod.GET,
	        entity,
	        PatientDTO[].class
	    );

	    // Récupérer le rôle de l'utilisateur à partir des en-têtes de la réponse
	    String userRoles = response.getHeaders().getFirst("X-User-Roles");
	    boolean isOrganisateur = userRoles != null && userRoles.contains("ROLE_ORGANISATEUR");

	    // Ajouter le rôle de l'utilisateur au modèle
	    model.addAttribute("isOrganisateur", isOrganisateur);

	    // Ajouter la liste des patients au modèle
	    List<PatientDTO> patients = Arrays.asList(response.getBody());
	    model.addAttribute("patients", patients);

	    // Retourner la vue Thymeleaf
	    return "afficher-patients";
	}


	@GetMapping("/afficher-details/{patientId}")
	public String afficherDetailsPatientWithRisk(@PathVariable Long patientId, Model model) {

		// Récupérer les détails du patient
		HttpEntity<String> entity = createHttpEntityWithBasicAuth();
		ResponseEntity<PatientDTO> response = restTemplate
				.exchange(urlMicroserviceGateway + "/patients/" + patientId, HttpMethod.GET, entity, PatientDTO.class);

		PatientDTO patient = response.getBody();

		// Récupérer les notes du médecin associées au patient
		ResponseEntity<MedecinNoteDTO[]> notesResponse = restTemplate.exchange(
				urlMicroserviceGateway + "/medecin/notes/" + patientId, HttpMethod.GET, entity, MedecinNoteDTO[].class);

		// Calculer le niveau de risque de diabète
		String diabetesRisk = restTemplate.exchange(urlMicroserviceGateway + "/diabetes-risk/patients/" + patientId,
				HttpMethod.GET, entity, String.class).getBody();

		// Ajouter les informations au modèle
		model.addAttribute("patient", patient);
		model.addAttribute("medecinNotes", Arrays.asList(notesResponse.getBody()));
		model.addAttribute("diabetesRisk", diabetesRisk);

	    // Récupérer le rôle de l'utilisateur à partir des en-têtes de la réponse
	    String userRoles = response.getHeaders().getFirst("X-User-Roles");
	    boolean isOrganisateur = userRoles != null && userRoles.contains("ROLE_ORGANISATEUR");

		model.addAttribute("isOrganisateur", isOrganisateur);

		return "afficher-details";
	}
	

	@GetMapping("/ajouter-patient")
	public String afficherFormulaireAjoutPatient(Model model) {
		model.addAttribute("newPatient", new PatientDTO());
		return "ajouter-patient";
	}

	@PostMapping("/ajouter-patient")
	public String ajouterPatient(@ModelAttribute("newPatient") PatientDTO newPatient, Model model) {

		// Créer une entité Http avec l'objet newPatient dans le corps de la requête
		HttpEntity<PatientDTO> entity = createHttpEntityWithBasicAuth(newPatient);

		String backendUrl = urlMicroserviceGateway + "/patients/add";
		// Envoyer une requête POST avec les données du nouveau patient

		ResponseEntity<PatientDTO> responseEntity = restTemplate.exchange(backendUrl, HttpMethod.POST, entity,
				PatientDTO.class);
		// Rediriger vers la page d'affichage des patients en cas de succès
		return "redirect:/afficher-patients";
	}

	@GetMapping("/modifier-adresse/{patientId}")
	public String afficherFormulaireModifierAdresse(@PathVariable Long patientId, Model model) {
		// Récupérer les détails du patient
		HttpEntity<String> entity = createHttpEntityWithBasicAuth();
		ResponseEntity<PatientDTO> patientResponse = restTemplate
				.exchange(urlMicroserviceGateway + "/patients/" + patientId, HttpMethod.GET, entity, PatientDTO.class);
		model.addAttribute("patient", patientResponse.getBody());

		return "modifier-adresse";
	}

	@PostMapping("/modifier-adresse/{patientId}")
	public String modifierAdressePatient(@PathVariable Long patientId, @RequestParam String nouvelleAdresse,
			Model model) {

		// Récupérer les détails du patient
		HttpEntity<String> entity = createHttpEntityWithBasicAuth();
		ResponseEntity<PatientDTO> patientResponse = restTemplate
				.exchange(urlMicroserviceGateway + "/patients/" + patientId, HttpMethod.GET, entity, PatientDTO.class);

		String backendUrl = urlMicroserviceGateway + "/patients/" + patientId + "/update-adresse?nouvelleAdresse="
				+ nouvelleAdresse;

		// Envoyer la requête pour mettre à jour l'adresse du patient
		ResponseEntity<PatientDTO> responseEntity = restTemplate.exchange(backendUrl, HttpMethod.POST, entity,
				PatientDTO.class);
		// Rediriger vers la page d'affichage des patients en cas de succès
		return "redirect:/afficher-patients";
	}

	@GetMapping("/modifier-numero/{patientId}")
	public String afficherFormulaireModifierNumero(@PathVariable Long patientId, Model model) {

		// Récupérer les détails du patient
		HttpEntity<String> entity = createHttpEntityWithBasicAuth();
		ResponseEntity<PatientDTO> patientResponse = restTemplate
				.exchange(urlMicroserviceGateway + "/patients/" + patientId, HttpMethod.GET, entity, PatientDTO.class);
		model.addAttribute("patient", patientResponse.getBody());
		return "modifier-numero";
	}

	@PostMapping("/modifier-numero/{patientId}")
	public String modifierNumeroPatient(@PathVariable Long patientId, @RequestParam String nouveauNumero, Model model) {

		// Récupérer les détails du patient
		HttpEntity<String> entity = createHttpEntityWithBasicAuth();
		ResponseEntity<PatientDTO> patientResponse = restTemplate
				.exchange(urlMicroserviceGateway + "/patients/" + patientId, HttpMethod.GET, entity, PatientDTO.class);
		// Construire l'URL du backend pour la modification du numéro de téléphone du
		// patient

		String backendUrl = urlMicroserviceGateway + "/patients/" + patientId + "/update-numero?nouveauNumero="
				+ nouveauNumero;
		// Envoyer la requête pour mettre à jour l'adresse du patient
		ResponseEntity<PatientDTO> responseEntity = restTemplate.exchange(backendUrl, HttpMethod.POST, entity,
				PatientDTO.class);

		// Rediriger vers la page d'affichage des détails du patient en cas de succès
		return "redirect:/afficher-details/" + patientId;
	}

	@GetMapping("/ajouter-note/{patientId}")
	public String afficherFormulaireAjoutNoteMedicale(@PathVariable Long patientId, Model model) {

		// Récupérer les détails du patient
		HttpEntity<String> entity = createHttpEntityWithBasicAuth();
		ResponseEntity<PatientDTO> patientResponse = restTemplate
				.exchange(urlMicroserviceGateway + "/patients/" + patientId, HttpMethod.GET, entity, PatientDTO.class);
		model.addAttribute("patient", patientResponse.getBody());
		return "ajouter-note";
	}

	@PostMapping("/ajouter-note/{patientId}")
	public String ajouterNoteMedicale(@PathVariable Long patientId, @RequestParam String nouvelleNote, Model model) {

		// Récupérer les détails du patient
		HttpEntity<String> entity = createHttpEntityWithBasicAuth();
		ResponseEntity<PatientDTO> patientResponse = restTemplate
				.exchange(urlMicroserviceGateway + "/patients/" + patientId, HttpMethod.GET, entity, PatientDTO.class);
		PatientDTO patient = patientResponse.getBody();

		// Créer un objet représentant la nouvelle note médicale
		MedecinNoteDTO nouvelleNoteMedicale = new MedecinNoteDTO();
		nouvelleNoteMedicale.setPatId(patientId);
		nouvelleNoteMedicale.setPatient(patient.getNom());
		nouvelleNoteMedicale.setNote(nouvelleNote);

		// Construire l'URL du backend pour l'ajout de la note médicale
		String backendUrl = urlMicroserviceGateway + "/medecin/notes/" + patientId;

		HttpEntity<MedecinNoteDTO> requestEntity = createHttpEntityWithBasicAuth(nouvelleNoteMedicale);

		ResponseEntity<MedecinNoteDTO> responseEntity = restTemplate.exchange(backendUrl, HttpMethod.POST,
				requestEntity, MedecinNoteDTO.class);

		return "redirect:/afficher-details/" + patientId;
	}

	@GetMapping("/error")
	public String afficherPageErreur(Model model) {
		return "error";
	}
}
