import http from "http";
import createHandler from "github-webhook-handler";
import { exec } from "child_process";
import { promisify } from "util";

const execAsync = promisify(exec);

// Configuration du webhook
// IMPORTANT: Changez ce secret dans un environnement de production
const WEBHOOK_SECRET = process.env.WEBHOOK_SECRET || "fe244359462a4e6944c5d631ff642d496ecc5e0d2485160c1803cabb49826175";
const WEBHOOK_PATH = process.env.WEBHOOK_PATH || "/webhook";
const PORT = process.env.WEBHOOK_PORT || 9000;

// Créer le handler GitHub
const handler = createHandler({
  path: WEBHOOK_PATH,
  secret: WEBHOOK_SECRET
});

// Gérer les événements de push
handler.on("push", async function (event) {
  console.log("📥 Webhook reçu - Push détecté");
  console.log(`📦 Repository: ${event.payload.repository.full_name}`);
  console.log(`🌿 Branche: ${event.payload.ref}`);
  console.log(`👤 Auteur: ${event.payload.head_commit.author.name}`);
  console.log(`💬 Commit: ${event.payload.head_commit.message}`);
  
  // Vérifier que c'est bien la branche main/master
  const branch = event.payload.ref.split("/").pop();
  if (branch !== "main" && branch !== "master") {
    console.log(`⚠️  Ignoré - Ce n'est pas la branche main/master (${branch})`);
    return;
  }

  try {
    console.log("🚀 Démarrage du déploiement...");
    const { stdout, stderr } = await execAsync("/opt/fi-thnity/backend/deploy.sh");
    
    if (stdout) console.log("✅ Output:", stdout);
    if (stderr) console.log("⚠️  Errors:", stderr);
    
    console.log("✅ Déploiement terminé avec succès!");
  } catch (error) {
    console.error("❌ Erreur lors du déploiement:", error);
  }
});

handler.on("error", function (err) {
  console.error("❌ Erreur webhook:", err.message);
});

// Créer le serveur HTTP
const server = http.createServer((req, res) => {
  handler(req, res, function (err) {
    res.statusCode = 404;
    res.end("❌ Webhook endpoint non trouvé");
  });
});

// Démarrer le serveur
server.listen(PORT, () => {
  console.log("=========================================");
  console.log("🔔 Serveur Webhook GitHub démarré");
  console.log(`📍 Port: ${PORT}`);
  console.log(`🔗 Path: ${WEBHOOK_PATH}`);
  console.log("=========================================");
});

