package com.microservices.dto;

import java.sql.Date;

public class PatientDTO {
    private Long id;
    private String prenom;
    private String nom;
    private Date dateNaissance;
    private String genre;
    private String adressePostale;
    private String numeroTelephone;

    public PatientDTO() {
    }

    public PatientDTO(Long id, String prenom, String nom, Date dateNaissance, String genre, String adressePostale, String numeroTelephone) {
        this.id = id;
        this.prenom = prenom;
        this.nom = nom;
        this.dateNaissance = dateNaissance;
        this.genre = genre;
        this.adressePostale = adressePostale;
        this.numeroTelephone = numeroTelephone;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Date getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(Date dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getAdressePostale() {
        return adressePostale;
    }

    public void setAdressePostale(String adressePostale) {
        this.adressePostale = adressePostale;
    }

    public String getNumeroTelephone() {
        return numeroTelephone;
    }

    public void setNumeroTelephone(String numeroTelephone) {
        this.numeroTelephone = numeroTelephone;
    }
}
