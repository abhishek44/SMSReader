/**
 * CategoryRepository — CRUD operations for the categories table.
 */

import {getDatabase, resultToArray} from './Database';

/**
 * Get all non-archived categories.
 * @returns {Promise<Array>}
 */
export async function getAllCategories() {
  const db = await getDatabase();
  const [result] = await db.executeSql(
    'SELECT * FROM categories WHERE is_archived = 0 ORDER BY name ASC',
  );
  return resultToArray(result);
}

/**
 * Get a single category by ID.
 * @param {string} id
 * @returns {Promise<Object|null>}
 */
export async function getCategoryById(id) {
  const db = await getDatabase();
  const [result] = await db.executeSql('SELECT * FROM categories WHERE id = ?', [id]);
  return result.rows.length > 0 ? result.rows.item(0) : null;
}

/**
 * Create a new category.
 * @param {Object} category - { name, type, icon, color }
 * @returns {Promise<string>} the new category id
 */
export async function createCategory(category) {
  const db = await getDatabase();
  const now = new Date().toISOString();
  const id = 'cat_' + Date.now().toString(36) + '_' + Math.random().toString(36).substr(2, 5);

  await db.executeSql(
    `INSERT INTO categories (id, name, type, icon, color, is_archived, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, 0, ?, ?)`,
    [
      id,
      category.name || '',
      category.type || 'expense',
      category.icon || '📦',
      category.color || '#90A4AE',
      now,
      now,
    ],
  );

  return id;
}

/**
 * Update an existing category.
 * @param {string} id
 * @param {Object} updates
 * @returns {Promise<void>}
 */
export async function updateCategory(id, updates) {
  const db = await getDatabase();
  const now = new Date().toISOString();

  await db.executeSql(
    `UPDATE categories SET name = ?, type = ?, icon = ?, color = ?, updated_at = ?
     WHERE id = ?`,
    [
      updates.name || '',
      updates.type || 'expense',
      updates.icon || '📦',
      updates.color || '#90A4AE',
      now,
      id,
    ],
  );
}

/**
 * Soft-delete (archive) a category.
 * @param {string} id
 * @returns {Promise<void>}
 */
export async function archiveCategory(id) {
  const db = await getDatabase();
  const now = new Date().toISOString();
  await db.executeSql(
    'UPDATE categories SET is_archived = 1, updated_at = ? WHERE id = ?',
    [now, id],
  );
}
