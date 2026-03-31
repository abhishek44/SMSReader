/**
 * TransactionRepository — CRUD operations for the transactions table.
 */

import {getDatabase, resultToArray} from './Database';

/**
 * Get all transactions, newest first.
 * @returns {Promise<Array>}
 */
export async function getAllTransactions() {
  const db = await getDatabase();
  const [result] = await db.executeSql(
    'SELECT * FROM transactions ORDER BY Date DESC',
  );
  return resultToArray(result);
}

/**
 * Get a transaction by ID.
 * @param {number} id
 * @returns {Promise<Object|null>}
 */
export async function getTransactionById(id) {
  const db = await getDatabase();
  const [result] = await db.executeSql('SELECT * FROM transactions WHERE Id = ?', [id]);
  return result.rows.length > 0 ? result.rows.item(0) : null;
}

/**
 * Check if a transaction already exists for a given SMS filename (to avoid duplicates).
 * @param {string} filename - unique key for the SMS (e.g., "sms_<id>")
 * @returns {Promise<boolean>}
 */
export async function transactionExistsForSms(filename) {
  const db = await getDatabase();
  const [result] = await db.executeSql(
    'SELECT COUNT(*) as cnt FROM transactions WHERE filename = ?',
    [filename],
  );
  return result.rows.item(0).cnt > 0;
}

/**
 * Save a reviewed transaction.
 * @param {Object} txn
 * @returns {Promise<number>} the insert ID
 */
export async function saveTransaction(txn) {
  const db = await getDatabase();
  const now = new Date().toISOString();

  const [result] = await db.executeSql(
    `INSERT INTO transactions
       (Date, Details, Debit, Credit, Account_name, Account_type, account_id,
        filename, review_status, review_datetime, uploaded_datetime,
        Category, category_id, Notes)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    [
      txn.date || now,
      txn.details || '',
      txn.debit || 0,
      txn.credit || 0,
      txn.account_name || '',
      txn.account_type || '',
      txn.account_id || '',
      txn.filename || '',
      'reviewed',
      now,                // review_datetime
      now,                // uploaded_datetime
      txn.category || '',
      txn.category_id || '',
      txn.notes || '',
    ],
  );

  return result.insertId;
}

/**
 * Update a transaction's review status.
 * @param {number} id
 * @param {string} status
 * @returns {Promise<void>}
 */
export async function updateReviewStatus(id, status) {
  const db = await getDatabase();
  const now = new Date().toISOString();
  await db.executeSql(
    'UPDATE transactions SET review_status = ?, review_datetime = ? WHERE Id = ?',
    [status, now, id],
  );
}

/**
 * Delete a transaction by ID.
 * @param {number} id
 * @returns {Promise<void>}
 */
export async function deleteTransaction(id) {
  const db = await getDatabase();
  await db.executeSql('DELETE FROM transactions WHERE Id = ?', [id]);
}
