/**
 * transactionParser — extracts structured financial data from SMS text.
 *
 * Attempts to extract:
 *   - amount (debit or credit)
 *   - date
 *   - account number / name
 *   - merchant / details
 *   - transaction type (debit/credit)
 *
 * This is a best-effort regex-based parser. It handles common Indian banking
 * SMS formats (HDFC, SBI, ICICI, Axis, Paytm, etc.) and generic patterns.
 */

/**
 * Parse a transaction SMS and return structured data.
 * @param {string} body - The SMS text
 * @param {number|string} smsDate - The SMS timestamp (ms)
 * @returns {Object} Parsed fields with best guesses
 */
export function parseTransactionSms(body, smsDate) {
  if (!body) {
    return getEmptyResult(smsDate);
  }

  const text = body.trim();
  const lower = text.toLowerCase();

  const result = {
    date: extractDate(text) || formatTimestamp(smsDate),
    details: extractDetails(text),
    debit: 0,
    credit: 0,
    account_name: extractAccountName(text),
    account_id: '',
    account_type: guessAccountType(text),
  };

  // Determine debit vs credit and extract amount
  const amount = extractAmount(text);
  const txnType = detectTransactionType(lower);

  if (txnType === 'credit') {
    result.credit = amount;
  } else {
    result.debit = amount;
  }

  return result;
}

/**
 * Detect whether the SMS describes a debit or credit transaction.
 */
function detectTransactionType(lower) {
  // Credit indicators
  const creditPatterns = [
    'credited', 'received', 'deposited', 'added', 'refund',
    'cashback', 'reversed', 'cr ', 'credit',
  ];
  // Debit indicators
  const debitPatterns = [
    'debited', 'spent', 'withdrawn', 'paid', 'sent', 'purchase',
    'deducted', 'dr ', 'debit', 'charged',
  ];

  for (const p of creditPatterns) {
    if (lower.includes(p)) return 'credit';
  }
  for (const p of debitPatterns) {
    if (lower.includes(p)) return 'debit';
  }

  return 'debit'; // default assumption
}

/**
 * Extract monetary amount from SMS text.
 * Looks for patterns like: Rs.1,234.56, Rs 1234, INR 500, ₹500
 */
function extractAmount(text) {
  // Common amount patterns (Indian format)
  const patterns = [
    /(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)/i,
    /(?:amount|amt)[\s:]*(?:rs\.?|inr|₹)?\s*([\d,]+(?:\.\d{1,2})?)/i,
    /([\d,]+(?:\.\d{1,2})?)\s*(?:rs|inr|₹)/i,
  ];

  for (const pattern of patterns) {
    const match = text.match(pattern);
    if (match) {
      const cleaned = match[1].replace(/,/g, '');
      const num = parseFloat(cleaned);
      if (!isNaN(num) && num > 0) {
        return num;
      }
    }
  }

  return 0;
}

/**
 * Extract a date from the SMS text.
 * Common formats: dd/mm/yy, dd-mm-yyyy, dd MMM yyyy, etc.
 * Returns YYYY-MM-DD string or null.
 */
function extractDate(text) {
  // dd/mm/yyyy or dd-mm-yyyy or dd.mm.yyyy
  const dateRegex1 = /(\d{1,2})[\/\-.](\d{1,2})[\/\-.](\d{2,4})/;
  const match1 = text.match(dateRegex1);
  if (match1) {
    const day = parseInt(match1[1], 10);
    const month = parseInt(match1[2], 10);
    let year = parseInt(match1[3], 10);
    if (year < 100) year += 2000;
    if (day >= 1 && day <= 31 && month >= 1 && month <= 12) {
      return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    }
  }

  // dd MMM yyyy or dd-MMM-yyyy
  const months = {
    jan: '01', feb: '02', mar: '03', apr: '04', may: '05', jun: '06',
    jul: '07', aug: '08', sep: '09', oct: '10', nov: '11', dec: '12',
  };
  const dateRegex2 = /(\d{1,2})[\s\-]?(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*[\s\-,]*(\d{2,4})/i;
  const match2 = text.match(dateRegex2);
  if (match2) {
    const day = parseInt(match2[1], 10);
    const month = months[match2[2].toLowerCase().substr(0, 3)];
    let year = parseInt(match2[3], 10);
    if (year < 100) year += 2000;
    if (month && day >= 1 && day <= 31) {
      return `${year}-${month}-${String(day).padStart(2, '0')}`;
    }
  }

  return null;
}

/**
 * Extract account number snippet from SMS.
 * Looks for patterns like: A/c XX1234, a/c ending 5678, account *1234
 */
function extractAccountName(text) {
  const patterns = [
    /(?:a\/c|acct?|account)\s*(?:no\.?\s*)?(?:ending\s+|[*xX]+)?\s*(\d{3,})/i,
    /(?:card|cc)\s*(?:ending\s+|no\.?\s+|[*xX]+)\s*(\d{3,})/i,
    /[*xX]+(\d{4})/,
  ];

  for (const pattern of patterns) {
    const match = text.match(pattern);
    if (match) {
      return `**${match[1]}`;
    }
  }

  return '';
}

/**
 * Guess the account type from SMS content.
 */
function guessAccountType(text) {
  const lower = text.toLowerCase();

  if (/credit\s*card|cc\s/i.test(lower)) return 'Credit Card';
  if (/wallet|paytm|phonepe|gpay/i.test(lower)) return 'Wallet';
  if (/savings|saving/i.test(lower)) return 'Savings';
  if (/current\s*a\/c|current\s*account/i.test(lower)) return 'Current';
  if (/upi|neft|imps|rtgs/i.test(lower)) return 'Savings';

  return 'Savings';
}

/**
 * Extract merchant or transaction details / description.
 * Tries to find "at <merchant>", "to <recipient>", "from <sender>", etc.
 */
function extractDetails(text) {
  // "at <merchant>" or "to <recipient>" or "from <sender>"
  const detailPatterns = [
    /(?:at|to|from|towards|for)\s+([A-Za-z0-9][\w\s&.'/-]{2,30}?)(?:\s+on|\s+ref|\s+via|\.|$)/i,
    /(?:info|ref|txn|utr)[\s:]+([A-Za-z0-9][\w\s/-]{2,40})/i,
  ];

  for (const pattern of detailPatterns) {
    const match = text.match(pattern);
    if (match) {
      return match[1].trim();
    }
  }

  // Fallback: return first 80 chars of the SMS as description
  return text.length > 80 ? text.substring(0, 80) + '...' : text;
}

/**
 * Format a timestamp (ms) to YYYY-MM-DD.
 */
function formatTimestamp(ts) {
  if (!ts) return new Date().toISOString().split('T')[0];
  const d = new Date(typeof ts === 'string' ? parseInt(ts, 10) : ts);
  return d.toISOString().split('T')[0];
}

/**
 * Return empty parsed result.
 */
function getEmptyResult(smsDate) {
  return {
    date: formatTimestamp(smsDate),
    details: '',
    debit: 0,
    credit: 0,
    account_name: '',
    account_id: '',
    account_type: 'Savings',
  };
}
