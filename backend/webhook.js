import http from "http";
import crypto from "crypto";
import { exec } from "child_process";
import { promisify } from "util";

const execAsync = promisify(exec);

// Configuration du webhook
const WEBHOOK_SECRET = process.env.WEBHOOK_SECRET || "fe244359462a4e6944c5d631ff642d496ecc5e0d2485160c1803cabb49826175";
const WEBHOOK_PATH = process.env.WEBHOOK_PATH || "/webhook";
const PORT = process.env.WEBHOOK_PORT || 9000;

// Fonction pour vérifier la signature GitHub
function verifySignature(payload, signature) {
  const hmac = crypto.createHmac("sha1", WEBHOOK_SECRET);
  const digest = "sha1=" + hmac.update(payload).digest("hex");
  return crypto.timingSafeEqual(Buffer.from(signature), Buffer.from(digest));
}

// Fonction de déploiement
async function deploy() {
  try {
    console.log("🚀 Démarrage du déploiement...");
    const { stdout, stderr } = await execAsync("/opt/fi-thnity/backend/deploy.sh");
    
    if (stdout) console.log("✅ Output:", stdout);
    if (stderr) console.log("⚠️  Errors:", stderr);
    
    console.log("✅ Déploiement terminé avec succès!");
    return { success: true, stdout, stderr };
  } catch (error) {
    console.error("❌ Erreur lors du déploiement:", error);
    return { success: false, error: error.message };
  }
}

// Créer le serveur HTTP
const server = http.createServer((req, res) => {
  // Vérifier que c'est bien le bon path
  if (req.url !== WEBHOOK_PATH) {
    res.statusCode = 404;
    res.end("❌ Webhook endpoint non trouvé");
    return;
  }

  // Vérifier que c'est une requête POST
  if (req.method !== "POST") {
    res.statusCode = 405;
    res.end("❌ Méthode non autorisée");
    return;
  }

  let body = "";
  const signature = req.headers["x-hub-signature"];

  // Vérifier la signature
  if (!signature) {
    console.warn("⚠️  Requête sans signature GitHub");
    res.statusCode = 401;
    res.end("❌ Signature manquante");
    return;
  }

  // Collecter le body
  req.on("data", (chunk) => {
    body += chunk.toString();
  });

  req.on("end", async () => {
    try {
      let payload;
      let rawPayload = body;

      // Si c'est x-www-form-urlencoded, extraire le payload
      if (req.headers["content-type"]?.includes("application/x-www-form-urlencoded")) {
        const params = new URLSearchParams(body);
        rawPayload = params.get("payload") || body;
      }

      // Vérifier la signature avec le payload brut
      if (!verifySignature(rawPayload, signature)) {
        console.error("❌ Signature invalide");
        res.statusCode = 401;
        res.end("❌ Signature invalide");
        return;
      }

      // Parser le JSON
      try {
        payload = JSON.parse(rawPayload);
      } catch (e) {
        console.error("❌ Erreur parsing JSON:", e.message);
        res.statusCode = 400;
        res.end("❌ Payload JSON invalide");
        return;
      }

      // Vérifier que c'est un événement push
      const event = req.headers["x-github-event"];
      if (event !== "push") {
        console.log(`ℹ️  Événement ignoré: ${event}`);
        res.statusCode = 200;
        res.end(`✅ Événement ${event} reçu mais ignoré`);
        return;
      }

      console.log("📥 Webhook reçu - Push détecté");
      console.log(`📦 Repository: ${payload.repository?.full_name || "unknown"}`);
      console.log(`🌿 Branche: ${payload.ref || "unknown"}`);
      console.log(`👤 Auteur: ${payload.head_commit?.author?.name || "unknown"}`);
      console.log(`💬 Commit: ${payload.head_commit?.message || "unknown"}`);

      // Vérifier que c'est bien la branche main/master
      const branch = payload.ref?.split("/").pop();
      if (branch !== "main" && branch !== "master") {
        console.log(`⚠️  Ignoré - Ce n'est pas la branche main/master (${branch})`);
        res.statusCode = 200;
        res.end(`✅ Push sur ${branch} ignoré`);
        return;
      }

      // Répondre immédiatement à GitHub
      res.statusCode = 200;
      res.setHeader("Content-Type", "application/json");
      res.end(JSON.stringify({ status: "received", message: "Déploiement en cours..." }));

      // Lancer le déploiement en arrière-plan
      deploy();

    } catch (error) {
      console.error("❌ Erreur:", error.message);
      res.statusCode = 500;
      res.end("❌ Erreur serveur");
    }
  });

  req.on("error", (err) => {
    console.error("❌ Erreur requête:", err.message);
    res.statusCode = 500;
    res.end("❌ Erreur serveur");
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
