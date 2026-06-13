const express = require("express");
const axios = require("axios");
const path = require("path");

const app = express();

app.use(express.urlencoded({ extended: true }));
app.use(express.json());
app.use(express.static(path.join(__dirname, "public")));

const JAVA_API_URL = "http://localhost:8081/auth";

app.get("/", (req, res) => {
  res.sendFile(path.join(__dirname, "public", "index.html"));
});

app.get("/verify", (req, res) => {
  res.sendFile(path.join(__dirname, "public", "verify.html"));
});

app.get("/dashboard", (req, res) => {
  res.sendFile(path.join(__dirname, "public", "dashboard.html"));
});

app.post("/send-code", async (req, res) => {
  const { email } = req.body;
  try {
    await axios.post(`${JAVA_API_URL}/request-code`, { email });
    res.redirect(`/verify?email=${encodeURIComponent(email)}`);
  } catch (error) {
    console.error("Erro ao solicitar código:", error.message);
    res.status(500).send("Erro ao solicitar o código. Tente novamente.");
  }
});

app.post("/verify-code", async (req, res) => {
  const { email, code } = req.body;
  try {
    const response = await axios.post(`${JAVA_API_URL}/verify-code`, {
      email,
      code,
    });
    res.json({ sucesso: true, token: response.data.token });
  } catch (error) {
    console.error("Erro ao validar código.");
    res
      .status(401)
      .json({ sucesso: false, mensagem: "Código inválido ou expirado." });
  }
});

const PORT = 3000;
app.listen(PORT, () => {
  console.log(`Servidor Node rodando em http://localhost:${PORT}`);
});
