INSERT INTO category (id, name, image_res_id) VALUES
 ('casa', 'Brechó de Casa', 'brecho-categories-house'),
 ('masculino', 'Brechó Masculino', 'categories-masculino'),
 ('feminino', 'Brechó Feminino', 'categories-feminino'),
 ('infantil', 'Brechó Infantil', 'categories-infantil'),
 ('luxo', 'Brechó de Luxo', 'categories-luxo'),
 ('designer', 'Brechó de Designer', 'categories-designer'),
 ('desapego', 'Brechó de Desapego', 'categories-desapego'),
 ('geral', 'Brechós Gerais', 'categories-geral')
ON CONFLICT (id) DO NOTHING;
