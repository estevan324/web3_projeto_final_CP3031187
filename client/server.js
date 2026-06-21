require("dotenv").config();
const express = require("express");
const axios = require("axios");
const path = require("path");

const app = express();

app.use(express.urlencoded({ extended: true }));
app.use(express.json());
app.use(express.static(path.join(__dirname, "public")));

const AUTH_API_URL = `${process.env.API_URL}/auth`;
const USER_API_URL = `${process.env.API_URL}/users`;

app.get("/", (req, res) => {
  res.sendFile(path.join(__dirname, "public", "index.html"));
});

app.get("/verify", (req, res) => {
  res.sendFile(path.join(__dirname, "public", "verify.html"));
});

app.get("/register", (req, res) => {
  res.sendFile(path.join(__dirname, "public", "register.html"));
});

app.get("/dashboard", (req, res) => {
  res.sendFile(path.join(__dirname, "public", "dashboard.html"));
});

app.post("/send-code", async (req, res) => {
  const { email } = req.body;
  try {
    await axios.post(`${AUTH_API_URL}/request-code`, { email });
    res.redirect(`/verify?email=${encodeURIComponent(email)}`);
  } catch (error) {
    console.error("Erro ao solicitar código:", error.message);
    res.status(500).send("Erro ao solicitar o código. Tente novamente.");
  }
});

app.post("/verify-code", async (req, res) => {
  const { email, code } = req.body;
  try {
    const response = await axios.post(`${AUTH_API_URL}/verify-code`, {
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

app.post("/update-profile", async (req, res) => {
  const { name, role, token } = req.body;

  if (!token) {
    return res
      .status(401)
      .send(
        "Sessão expirada ou token não fornecido. Volte para a página inicial.",
      );
  }

  try {
    const response = await axios.post(
      `${USER_API_URL}/update-profile`,
      { name, role },
      {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      },
    );

    res.redirect("/dashboard");
  } catch (error) {
    console.error("Erro ao atualizar perfil no backend Java:", error.message);

    res.status(error.response?.status || 500).send(
      `<h3>Erro ao atualizar o perfil</h3>
       <p>${error.response?.data?.message || "Erro interno do servidor."}</p>
       <a href="/register">Tentar novamente</a>`,
    );
  }
});

app.get("/api/protected", async (req, res) => {
  const authHeader = req.headers.authorization;

  if (!authHeader) {
    return res.status(401).send("Token não fornecido");
  }

  try {
    const response = await axios.get(`${USER_API_URL}/test/customer`, {
      headers: {
        Authorization: authHeader,
      },
    });

    res.send(response.data);
  } catch (error) {
    console.error("Erro ao acessar rota protegida:", error.message);
    res
      .status(error.response?.status || 500)
      .send("Acesso negado ou erro no servidor principal.");
  }
});

app.get("/api/me", async (req, res) => {
  const authHeader = req.headers.authorization;

  if (!authHeader) {
    return res.status(401).send("Token não fornecido");
  }

  try {
    const response = await axios.get(`${USER_API_URL}/me`, {
      headers: {
        Authorization: authHeader,
      },
    });

    res.json(response.data);
  } catch (error) {
    console.error("Erro ao buscar perfil no backend Java:", error.message);
    res
      .status(error.response?.status || 500)
      .send("Erro ao buscar os dados do usuário.");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Servidor Node rodando em http://localhost:${PORT}`);
});
