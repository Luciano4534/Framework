<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Upload de fichier</title>
</head>
<body>
    <h1>Uploader un fichier</h1>
    <form action="${pageContext.request.contextPath}/employe/upload" method="post"
          enctype="multipart/form-data">
        <p>
            <label for="fichier">Fichier :</label>
            <input type="file" id="fichier" name="fichier"/>
        </p>
        <p>
            <label for="description">Description :</label>
            <input type="text" id="description" name="description"/>
        </p>
        <button type="submit">Envoyer</button>
    </form>
</body>
</html>
