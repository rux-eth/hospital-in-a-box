const express = require("express");
const app = express();
const port = process.env.PORT || 3000;

app.get("/", (_req, res) => {
  res.send("hospital-in-a-box web placeholder");
});

app.listen(port, () => {
  console.log(`Web server listening on port ${port}`);
});

module.exports = app;
