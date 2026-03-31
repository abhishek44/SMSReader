/**
 * SMS Service — reads SMS messages from the device.
 *
 * Uses a custom native module (SmsModule) to query Android's
 * content://sms/inbox content provider.
 *
 * Handles:
 * - Runtime permission requests (Android 6+)
 * - Address normalization for grouping
 * - Conversation construction from flat message list
 */

import {NativeModules, PermissionsAndroid, Platform} from 'react-native';
import {classifyMessage} from '../utils/classifier';

const {SmsModule} = NativeModules;

/**
 * Normalize a phone address for grouping.
 * Strips all non-digit chars (keeps leading +), lowercases alpha senders.
 * @param {string} address
 * @returns {string}
 */
function normalizeAddress(address) {
  if (!address) return 'unknown';

  // If the address is purely alphabetic/alphanumeric (like "HDFCBK"),
  // normalize to uppercase for consistent grouping
  if (/^[A-Za-z]/.test(address)) {
    return address.toUpperCase().trim();
  }

  // For phone numbers, strip everything except digits and leading +
  const cleaned = address.replace(/[^\d+]/g, '');
  // Remove country code prefix for Indian numbers for better grouping
  // +91XXXXXXXXXX and XXXXXXXXXX should group together
  if (cleaned.startsWith('+91') && cleaned.length === 13) {
    return cleaned.substring(3);
  }
  if (cleaned.startsWith('91') && cleaned.length === 12) {
    return cleaned.substring(2);
  }
  return cleaned || address.trim();
}

/**
 * Request READ_SMS permission on Android.
 * @returns {Promise<boolean>} true if granted
 */
async function requestSmsPermission() {
  if (Platform.OS !== 'android') {
    return false;
  }

  try {
    const granted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.READ_SMS,
      {
        title: 'SMS Permission',
        message: 'SMS Reader needs access to your messages to display them.',
        buttonPositive: 'Allow',
        buttonNegative: 'Deny',
        buttonNeutral: 'Ask Later',
      },
    );
    return granted === PermissionsAndroid.RESULTS.GRANTED;
  } catch (err) {
    console.warn('Permission request error:', err);
    return false;
  }
}

/**
 * Fetch SMS messages from the device and group them into conversations.
 *
 * @param {number} limit - Maximum number of messages to fetch (default 500)
 * @returns {Promise<{conversations: Array, error: string|null}>}
 *
 * Each conversation object:
 * {
 *   address: string,           // normalized sender address
 *   displayAddress: string,    // original sender for display
 *   messages: Array,           // sorted newest-first
 *   lastMessage: string,       // body of newest message
 *   lastDate: number,          // timestamp of newest message
 *   category: string,          // 'transaction' | 'promotion' | 'personal'
 *   messageCount: number,
 * }
 */
export async function fetchConversations(limit = 500) {
  // Step 1: Request permission
  const hasPermission = await requestSmsPermission();
  if (!hasPermission) {
    return {conversations: [], error: 'permission_denied'};
  }

  // Step 2: Fetch raw messages from native module
  try {
    const rawMessages = await SmsModule.getMessages(limit);

    if (!rawMessages || rawMessages.length === 0) {
      return {conversations: [], error: null};
    }

    // Step 3: Group by normalized address
    const grouped = {};

    rawMessages.forEach(msg => {
      const normalizedAddr = normalizeAddress(msg.address);

      if (!grouped[normalizedAddr]) {
        grouped[normalizedAddr] = {
          address: normalizedAddr,
          displayAddress: msg.address || 'Unknown',
          messages: [],
        };
      }
      grouped[normalizedAddr].messages.push({
        id: msg.id,
        body: msg.body || '',
        date: parseInt(msg.date, 10),
        type: msg.type, // 1 = inbox, 2 = sent
        read: msg.read === '1',
        category: classifyMessage(msg.body),
      });
    });

    // Step 4: Sort messages within each conversation & build summary
    const conversations = Object.values(grouped).map(conv => {
      // Sort messages newest-first
      conv.messages.sort((a, b) => b.date - a.date);

      const latestMsg = conv.messages[0];
      return {
        address: conv.address,
        displayAddress: conv.displayAddress,
        messages: conv.messages,
        lastMessage: latestMsg.body,
        lastDate: latestMsg.date,
        category: latestMsg.category,
        messageCount: conv.messages.length,
      };
    });

    // Step 5: Sort conversations newest-first
    conversations.sort((a, b) => b.lastDate - a.lastDate);

    return {conversations, error: null};
  } catch (err) {
    console.error('Failed to fetch SMS:', err);
    return {conversations: [], error: 'fetch_failed'};
  }
}
