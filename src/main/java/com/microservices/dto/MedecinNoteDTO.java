package com.microservices.dto;

public class MedecinNoteDTO {

    private String id;
    private int patId;
    private String patient;
    private String note;

	public MedecinNoteDTO() {
    }

	public MedecinNoteDTO(String id, int patId, String patient, String note) {
        this.id = id;
        this.patId = patId;
        this.patient = patient;
        this.note = note;
    }

    public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	public int getPatId() {
		return patId;
	}


	public void setPatId(int patId) {
		this.patId = patId;
	}


	public String getPatient() {
		return patient;
	}


	public void setPatient(String patient) {
		this.patient = patient;
	}


	public String getNote() {
		return note;
	}


	public void setNote(String note) {
		this.note = note;
	}
}
