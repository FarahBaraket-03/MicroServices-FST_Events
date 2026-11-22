FST Events • Démonstration locale

1. Extraire le dossier demo-console dans events-microservices, à côté de compose.yml.
2. Démarrer le projet comme d’habitude avec Docker Desktop. Le lanceur utilise
   les ports du projet (8090, 8092), Docker Compose et Python 3.8+ pour Windows.
3. Double-cliquer sur Lancer.cmd. La console ouvre http://127.0.0.1:8787.
4. Cliquer sur Statistiques de l’événement puis sur Tout exécuter.
   Cliquer sur chaque étape pour voir la réponse HTTP. Les pannes et leur
   restauration peuvent prendre plusieurs minutes. Le parcours s’arrête au
   premier échec. Lire « À vérifier », puis relancer le scénario concerné.

Le lanceur réutilise reports/demo-ids.json. S’il manque, le script smoke.ps1
crée une fois les données métier. Si vous avez supprimé cet événement, exécutez
scripts/smoke.ps1 pour enregistrer de nouveaux identifiants. CQRS utilise une
ressource temporaire distincte. Le test du débit peut dépendre du timing.

Pendant la présentation, gardez le lanceur ouvert. Les résultats sont conservés
à l’écran jusqu’au prochain lancement d’un scénario. Ouvrez Grafana pour les
métriques et Zipkin pour retrouver une requête. Pour Axon, copiez l’identifiant
idLog de la commande dans la vue des événements. Jenkins, Nexus et SonarQube
ouvrent vos outils existants pour présenter vos propres preuves de livraison.

Après une interruption forcée, ouvrir PowerShell dans events-microservices :
docker compose start mysql-logistics
docker compose up -d --wait logistics-service event-service
Si la seconde instance était arrêtée avant le test de répartition :
docker compose --profile scale stop participant-service-2
Puis vérifier les statistiques normales. Ces commandes gardent les volumes.
