package tn.fst.logisticsservice;

import jakarta.persistence.*;
import java.math.BigDecimal;
import tn.fst.shared.Models.LogisticsView;

@Entity
@Table(name = "logistics_projection")
public class LogisticsReadModel {
  @Id public String id;

  @Column(nullable = false)
  public String description;

  public boolean reserve;

  @Column(precision = 12, scale = 2, nullable = false)
  public BigDecimal prixUnit;

  public int quantite;

  public LogisticsView view() {
    return new LogisticsView(id, description, reserve, prixUnit, quantite);
  }
}
