package tn.fst.participantservice;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.fst.shared.Models.*;

@RestController
@RequestMapping("/participants")
public class ParticipantController {
  private final ParticipantRepository repository;

  public ParticipantController(ParticipantRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public List<ParticipantView> all(@RequestParam(required = false) Set<Long> ids) {
    return (ids == null ? repository.findAll() : repository.findAllById(ids))
        .stream().map(Participant::view).toList();
  }

  @GetMapping("/{id}")
  public ParticipantView get(@PathVariable Long id) {
    return find(id).view();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ParticipantView create(@Valid @RequestBody ParticipantInput input) {
    Participant p = new Participant();
    p.update(input);
    return repository.save(p).view();
  }

  @PutMapping("/{id}")
  public ParticipantView update(@PathVariable Long id, @Valid @RequestBody ParticipantInput input) {
    Participant p = find(id);
    p.update(input);
    return repository.save(p).view();
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    repository.delete(find(id));
  }

  private Participant find(Long id) {
    return repository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant not found"));
  }
}
