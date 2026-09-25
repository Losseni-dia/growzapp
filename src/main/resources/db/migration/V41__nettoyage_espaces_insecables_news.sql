-- L'editeur riche (Quill) a insere des espaces insecables (U+00A0, ou
-- l'entite "&nbsp;") a de nombreux endroits dans les articles existants.
-- Un espace insecable empeche le retour a la ligne a cet endroit, ce qui
-- donnait un texte mal reparti sur les lignes dans toutes les langues.
UPDATE news
SET
    title = REPLACE(REPLACE(title, chr(160), ' '), '&nbsp;', ' '),
    content = REPLACE(REPLACE(content, chr(160), ' '), '&nbsp;', ' ');
