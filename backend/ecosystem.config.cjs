module.exports = {
  apps: [
    {
      name: "fi-thnity-backend",
      script: "npm",
      args: "run dev",
      cwd: "/opt/fi-thnity/backend",
      watch: true,
      ignore_watch: ["node_modules", ".git", "logs"],
      env: {
        NODE_ENV: "development"
      },
      error_file: "/opt/fi-thnity/backend/logs/backend-error.log",
      out_file: "/opt/fi-thnity/backend/logs/backend-out.log",
      log_date_format: "YYYY-MM-DD HH:mm:ss Z",
      merge_logs: true,
      autorestart: true,
      max_memory_restart: "1G"
    },
    {
      name: "fi-thnity-webhook",
      script: "webhook.js",
      cwd: "/opt/fi-thnity/backend",
      interpreter: "node",
      env: {
        WEBHOOK_SECRET: "fe244359462a4e6944c5d631ff642d496ecc5e0d2485160c1803cabb49826175",
        WEBHOOK_PATH: "/webhook",
        WEBHOOK_PORT: 9000
      },
      error_file: "/opt/fi-thnity/backend/logs/webhook-error.log",
      out_file: "/opt/fi-thnity/backend/logs/webhook-out.log",
      log_date_format: "YYYY-MM-DD HH:mm:ss Z",
      merge_logs: true,
      autorestart: true
    }
  ]
};

