-- Reference data for production (safe, minimal)
-- Categories only; no sample stores or demo content
INSERT INTO category (id, name_string_id, image_res_id) VALUES
 ('casa', 'brecho_de_casa', 'brecho-categories-house'),
 ('masculino', 'brecho_masculino', 'categories-masculino'),
 ('feminino', 'brecho_feminino', 'categories-feminino'),
 ('infantil', 'brecho_infantil', 'categories-infantil'),
 ('luxo', 'brecho_de_luxo', 'categories-luxo'),
 ('designer', 'brecho_de_designer', 'categories-designer'),
 ('desapego', 'brecho_de_desapego', 'categories-desapego'),
 ('geral', 'brechos_gerais', 'categories-geral')
ON CONFLICT (id) DO NOTHING;
