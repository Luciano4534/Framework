<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Profil</title>
</head>
<body>
    <h1>${message}</h1>
    <p>Rôles : ${roles}</p>
    <ul>
        <li><a href="${pageContext.request.contextPath}/profil">Profil</a></li>
        <li><a href="${pageContext.request.contextPath}/prof/cours">Cours (prof)</a></li>
        <li><a href="${pageContext.request.contextPath}/admin">Admin</a></li>
        <li><a href="${pageContext.request.contextPath}/logout">Déconnexion</a></li>
    </ul>
</body>
</html>
