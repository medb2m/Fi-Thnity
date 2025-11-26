#!/bin/bash

# Script de déploiement automatique
# Exécuté par le webhook GitHub lors d'un push

set -e  # Arrêter en cas d'erreur

echo "========================================="
echo "🚀 Déploiement automatique démarré"
echo "Date: $(date)"
echo "========================================="

# Aller dans le répertoire du backend
cd /opt/fi-thnity/backend

# Afficher la branche actuelle
echo "📋 Branche actuelle: $(git branch --show-current)"

# Pull les dernières modifications
echo "⬇️  Récupération des dernières modifications..."
git pull origin main || git pull origin master

# Installer les nouvelles dépendances (avec dev dependencies pour nodemon)
echo "📦 Installation des dépendances..."
npm install

# Redémarrer l'application avec PM2
echo "🔄 Redémarrage de l'application..."
pm2 restart fi-thnity-backend || pm2 start npm --name "fi-thnity-backend" -- run dev

echo "✅ Déploiement terminé avec succès!"
echo "========================================="

