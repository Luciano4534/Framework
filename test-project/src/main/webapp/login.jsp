<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Connexion</title>
</head>
<body>
    <h1>Connexion</h1>
    <p style="color:red">${error}</p>
    <p>${message}</p>
    <form action="${pageContext.request.contextPath}/login" method="post">
        <p>
            <label for="username">Utilisateur :</label>
            <input type="text" id="username" name="username"/>
        </p>
        <p>
            <label for="password">Mot de passe :</label>
            <input type="password" id="password" name="password"/>
        </p>
        <button type="submit">Se connecter</button>
    </form>
    <p>Comptes de démonstration : admin, prof, etudiant (mot de passe libre).</p>
</body>
</html>
