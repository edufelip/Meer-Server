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

-- Seed thrift stores (dev preview data)
INSERT INTO thrift_store (id, name, tagline, cover_image_url, address_line, latitude, longitude, map_image_url,
                          opening_hours, opening_hours_notes, facebook, instagram, website, whatsapp,
                          neighborhood, badge_label, is_favorite, description)
VALUES
 ('00000000-0000-0000-0000-000000000001', 'Vintage Vibes', 'Garimpos curados com pegada retrô',
  'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=1600&q=80',
  'Rua Augusta, 123 - Pinheiros', -23.5617, -46.6589,
  'https://lh3.googleusercontent.com/aida-public/AB6AXuATD7G9oKF2W1aQjxAqHpYvVPamBIvCIZ6Q7I74RHrH7zwrJHn7iFRGMdMEHWLTMlP9DQ7oquk7Frb_j9QaIiT7ZSYMjZJhvTjFAJU7U-X73PmboiSxOHwS4QZ9mIBO-fJWAbwbdWu5yfwTrXn0c6HHGRpI5fDlZ_HckG3G5-IAsF_Vsh98T6DdyXbPl0bdG-iC9J2bjl6tqGgQIoeItBfJUqcnWgrKl9Y05nEY0VjB15UkZf5t6v0xiO0VVOuXFpoAn1Z7WNfG-dc',
  'Seg a Sáb: 10:00 - 19:00', 'Fecha aos domingos', NULL, '@vintagevibes', NULL, NULL,
  'Pinheiros', 'Mais amado', true, 'Peças icônicas dos anos 70 e 80, curadoria semanal.'),

 ('00000000-0000-0000-0000-000000000002', 'Secondhand Chic', 'Peças de grife em segunda mão',
  'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=1600&q=80',
  'Av. Paulista, 456 - Bela Vista', -23.5614, -46.6558,
  NULL,
  'Seg a Sex: 11:00 - 18:00', NULL, NULL, '@secondhandchic', '@secondhandchic', NULL,
  'Vila Madalena', NULL, false, 'Grifes seminovas autenticadas e em ótimo estado.'),

 ('00000000-0000-0000-0000-000000000003', 'Thrift Haven', 'Achadinhos baratos e estilosos',
  'https://images.unsplash.com/photo-1469334031218-e382a71b716b?auto=format&fit=crop&w=1600&q=80',
  'Rua 7 de Abril, 90 - Centro', -23.546, -46.638,
  NULL,
  'Ter a Dom: 10:00 - 20:00', NULL, NULL, '@thrifthaven', '@thrifthaven', NULL,
  'Centro', NULL, false, 'Racks sempre renovados com preços acessíveis.'),

 ('00000000-0000-0000-0000-000000000004', 'Eco Trends', 'Slow fashion e peças eco-friendly',
  'https://images.unsplash.com/photo-1503341455253-b2e723bb3dbb?auto=format&fit=crop&w=1600&q=80',
  'Alameda dos Maracatins, 300 - Moema', -23.6101, -46.6675,
  NULL,
  'Seg a Sáb: 11:00 - 19:00', NULL, NULL, '@ecotrends', '@ecotrends', NULL,
  'Augusta', NULL, false, 'Curadoria eco-friendly com marcas locais.'),

 ('00000000-0000-0000-0000-000000000005', 'Garimpo Urbano', 'Street + vintage na Consolação',
  'https://images.unsplash.com/photo-1503341455253-b2e723bb3dbb?auto=format&fit=crop&w=1600&q=80',
  'Rua da Consolação, 210', -23.5565, -46.6623,
  NULL,
  'Seg a Sáb: 10:00 - 19:30', NULL, NULL, '@garimpourbano', '@garimpourbano', NULL,
  'Augusta', NULL, false, NULL),

 ('00000000-0000-0000-0000-000000000006', 'Querido Brechó', 'Curadoria feminina e genderless',
  'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=1600&q=80',
  'Rua Sabará, 41', -23.5610, -46.6682,
  NULL,
  'Seg a Sex: 11:00 - 18:30', NULL, NULL, '@queridobrecho', '@queridobrecho', NULL,
  'Pinheiros', NULL, false, NULL),

 ('00000000-0000-0000-0000-000000000007', 'Revive Vintage', 'Peças clássicas restauradas',
  'https://images.unsplash.com/photo-1509631179647-0177331693ae?auto=format&fit=crop&w=1600&q=80',
  'Rua São Carlos do Pinhal, 15', -23.5588, -46.6510,
  NULL,
  'Qua a Dom: 12:00 - 20:00', NULL, NULL, '@revivevintage', '@revivevintage', NULL,
  'Centro', NULL, false, NULL),

 ('00000000-0000-0000-0000-000000000008', 'Vintage Chic', 'Clássicos curados',
  'https://images.unsplash.com/photo-1496747611180-206a5c8c1a09?auto=format&fit=crop&w=1600&q=80',
  'Rua Exemplo, 99', -23.5599, -46.6601,
  NULL,
  'Seg a Sex: 10:00 - 18:00', NULL, NULL, '@vintagechic', '@vintagechic', NULL,
  'Centro', NULL, false, NULL)
ON CONFLICT (id) DO NOTHING;

-- Gallery URLs
INSERT INTO thrift_store_gallery_urls (thrift_store_id, gallery_urls) VALUES
 ('00000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1509631179647-0177331693ae?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000002', 'https://images.unsplash.com/photo-1496747611180-206a5c8c1a09?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000002', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000002', 'https://images.unsplash.com/photo-1496747611180-206a5c8c1a09?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000003', 'https://images.unsplash.com/photo-1524606894343-1e1a4c71d692?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000003', 'https://images.unsplash.com/photo-1496747611180-206a5c8c1a09?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000003', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000004', 'https://images.unsplash.com/photo-1509631179647-0177331693ae?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000004', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000004', 'https://images.unsplash.com/photo-1496747611180-206a5c8c1a09?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000005', 'https://images.unsplash.com/photo-1469334031218-e382a71b716b?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000005', 'https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000005', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000006', 'https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000006', 'https://images.unsplash.com/photo-1496747611180-206a5c8c1a09?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000006', 'https://images.unsplash.com/photo-1524606894343-1e1a4c71d692?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000007', 'https://images.unsplash.com/photo-1503341455253-b2e723bb3dbb?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000007', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=80'),
 ('00000000-0000-0000-0000-000000000007', 'https://images.unsplash.com/photo-1496747611180-206a5c8c1a09?auto=format&fit=crop&w=800&q=80')
ON CONFLICT DO NOTHING;

-- Categories per store
INSERT INTO thrift_store_categories (thrift_store_id, categories) VALUES
 ('00000000-0000-0000-0000-000000000001', 'Feminino'), ('00000000-0000-0000-0000-000000000001', 'Vintage'), ('00000000-0000-0000-0000-000000000001', 'Acessórios'),
 ('00000000-0000-0000-0000-000000000002', 'Feminino'), ('00000000-0000-0000-0000-000000000002', 'Luxo'),
 ('00000000-0000-0000-0000-000000000003', 'Vintage'), ('00000000-0000-0000-0000-000000000003', 'Casual'),
 ('00000000-0000-0000-0000-000000000004', 'Sustentável'), ('00000000-0000-0000-0000-000000000004', 'Unissex'),
 ('00000000-0000-0000-0000-000000000005', 'Street'), ('00000000-0000-0000-0000-000000000005', 'Vintage'),
 ('00000000-0000-0000-0000-000000000006', 'Feminino'), ('00000000-0000-0000-0000-000000000006', 'Genderless'),
 ('00000000-0000-0000-0000-000000000007', 'Vintage'), ('00000000-0000-0000-0000-000000000007', 'Restauração')
ON CONFLICT DO NOTHING;

-- Guide content/articles (mapped to store 8: Vintage Chic)
INSERT INTO guide_content (id, title, description, category_label, type, image_url, thrift_store_id) VALUES
 (1, 'Dicas para um garimpo de sucesso', 'Aprenda a encontrar as melhores peças em brechós.', 'Guia de estilo', 'article',
  'https://lh3.googleusercontent.com/aida-public/AB6AXuASh-dosAr4TVNex49PUkBKFWcLJ5g7HOQJC7p6SaRZyNznaks3TiQuWOksGvrnYi6IeO5sMPBjaerUTI7HzO4xaF5jyAX9NkZS80VOX-lpdnJIHTDaM3nL8ANPzyy3T2OEPfbqu2cuQlC-_PdB_tFutmEey75ynvkAcO3CQis8asojk9mkENmn1Hg88uqHJEOxr2z8LyIELsQfsWo_vVdfdLbws8VFobNPLNE5cMP-Snp3CsMplvntxVg4BQTHBAk7pgXTv1Px3Ls',
  '00000000-0000-0000-0000-000000000008'),
 (2, 'Tour pelo brechó: novidades da semana', 'Veja os achados que chegaram nesta semana no nosso espaço.', 'Vídeo', 'article',
  'https://lh3.googleusercontent.com/aida-public/AB6AXuBmfZnwoz2FyAt09OQcXxSOWFu-nzy3A0N7Y8dSUZmyVxMgOCUCY3KYMJkp8VWQmVt_eEPzL69-jCYmwaqq52hdiViVNBeuOIy35QjmobKVUKmv0XujKAj04kCQYbEijnAQ84NSgvQ2618bew_ido3S_RsVHz9SiI3adBYqbWWILbs5CkJk5nZnxcQK4mTayuijLFbB_TIx3KsrW2ONKeAeFmq2bGqv-C1QFs-D3V9stPlgaG84Q31ythQGOQjNwpsW6FmOwIdo2us',
  '00000000-0000-0000-0000-000000000008'),
 (3, 'Como cuidar das suas peças vintage', 'Guia rápido para conservar tecidos delicados e couro antigo.', 'Dica Rápida', 'article',
  'https://lh3.googleusercontent.com/aida-public/AB6AXuBmfZnwoz2FyAt09OQcXxSOWFu-nzy3A0N7Y8dSUZmyVxMgOCUCY3KYMJkp8VWQmVt_eEPzL69-jCYmwaqq52hdiViVNBeuOIy35QjmobKVUKmv0XujKAj04kCQYbEijnAQ84NSgvQ2618bew_ido3S_RsVHz9SiI3adBYqbWWILbs5CkJk5nZnxcQK4mTayuijLFbB_TIx3KsrW2ONKeAeFmq2bGqv-C1QFs-D3V9stPlgaG84Q31ythQGOQjNwpsW6FmOwIdo2us',
  '00000000-0000-0000-0000-000000000008'),
 (4, 'Look do dia com achados do brechó', 'Inspire-se com combinações práticas para o cotidiano.', 'Post', 'article',
  'https://lh3.googleusercontent.com/aida-public/AB6AXuBmfZnwoz2FyAt09OQcXxSOWFu-nzy3A0N7Y8dSUZmyVxMgOCUCY3KYMJkp8VWQmVt_eEPzL69-jCYmwaqq52hdiViVNBeuOIy35QjmobKVUKmv0XujKAj04kCQYbEijnAQ84NSgvQ2618bew_ido3S_RsVHz9SiI3adBYqbWWILbs5CkJk5nZnxcQK4mTayuijLFbB_TIx3KsrW2ONKeAeFmq2bGqv-C1QFs-D3V9stPlgaG84Q31ythQGOQjNwpsW6FmOwIdo2us',
  '00000000-0000-0000-0000-000000000008')
ON CONFLICT (id) DO NOTHING;
