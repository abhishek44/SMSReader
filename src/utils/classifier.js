/**
 * Rule-based SMS message classifier.
 *
 * Categories:
 *   - transaction: banking, OTP, payment, etc.
 *   - promotion:   sales, offers, discounts, etc.
 *   - personal:    anything that doesn't match the above
 *
 * Classification priority: transaction > promotion > personal
 */

const TRANSACTION_KEYWORDS = [
  'bank',
  'otp',
  'payment',
  'debit',
  'credit',
  'order',
  'invoice',
  'recharge',
  'transaction',
  'transfer',
  'upi',
  'neft',
  'imps',
  'account',
  'balance',
  'withdraw',
  'deposit',
  'emi',
  'loan',
  'a/c',
  'atm',
  'credited',
  'debited',
];

const PROMOTION_KEYWORDS = [
  'sale',
  'offer',
  'discount',
  'deal',
  'promo',
  'shop',
  'free',
  'limited time',
  'cashback',
  'coupon',
  'win',
  'reward',
  'exclusive',
  'subscribe',
  'buy now',
  'hurry',
  'flat',
  'off on',
  '% off',
];

// Build regex patterns once (case-insensitive)
const transactionRegex = new RegExp(
  TRANSACTION_KEYWORDS.map(k => k.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|'),
  'i',
);

const promotionRegex = new RegExp(
  PROMOTION_KEYWORDS.map(k => k.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|'),
  'i',
);

/**
 * Classify a single message body into a category.
 * @param {string} body - The SMS message text.
 * @returns {'transaction' | 'promotion' | 'personal'}
 */
export function classifyMessage(body) {
  if (!body) return 'personal';

  // Short-code / alphanumeric senders are typically non-personal,
  // but we classify based on content for accuracy.

  if (transactionRegex.test(body)) {
    return 'transaction';
  }

  if (promotionRegex.test(body)) {
    return 'promotion';
  }

  return 'personal';
}

/**
 * Classify an entire conversation based on its most recent message.
 * @param {Array} messages - Array of message objects with `body` field.
 * @returns {'transaction' | 'promotion' | 'personal'}
 */
export function classifyConversation(messages) {
  if (!messages || messages.length === 0) return 'personal';

  // Use the most recent message for classification
  return classifyMessage(messages[0].body);
}

/**
 * Get display label for a category.
 * @param {string} category
 * @returns {string}
 */
export function getCategoryLabel(category) {
  const labels = {
    transaction: 'Transaction',
    promotion: 'Promotion',
    personal: 'Personal',
  };
  return labels[category] || 'Personal';
}
