<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Formulaire employé</title>
</head>
<body>
    <h1>Ajouter un employé</h1>
    <form action="${pageContext.request.contextPath}/employe/save" method="post">
        <p>
            <label for="nom">Nom :</label>
            <input type="text" id="nom" name="nom"/>
        </p>
        <p>
            <label for="age">Age :</label>
            <input type="number" id="age" name="age"/>
        </p>
        <p>
            <label for="salaire">Salaire :</label>
            <input type="number" step="0.01" id="salaire" name="salaire"/>
        </p>
        <button type="submit">Soumettre</button>
    </form>
</body>
</html>
