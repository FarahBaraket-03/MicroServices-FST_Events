package tn.fst.participantservice;

import jakarta.persistence.*;
import tn.fst.shared.Models.*;

@Entity
@Table(name = "participants")
public class Participant {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public String nom;

  @Column(nullable = false)
  public String prenom;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Tache tache;

  public ParticipantView view() {
    return new ParticipantView(id, nom, prenom, tache);
  }

  public void update(ParticipantInput input) {
    nom = input.nom().trim();
    prenom = input.prenom().trim();
    tache = input.tache();
  }
}
