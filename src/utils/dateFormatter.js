/**
 * Date formatting utilities for the SMS reader.
 * Converts Unix timestamps (ms) to human-readable strings.
 */

/**
 * Format a timestamp for the conversation list.
 * Shows "Today", "Yesterday", or the date.
 * @param {number|string} timestamp - Unix timestamp in milliseconds
 * @returns {string}
 */
export function formatConversationDate(timestamp) {
  const date = new Date(parseInt(timestamp, 10));
  const now = new Date();

  // Strip time for comparison
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const msgDay = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const diffDays = Math.floor((today - msgDay) / (1000 * 60 * 60 * 24));

  if (diffDays === 0) {
    return formatTime(date);
  }

  if (diffDays === 1) {
    return 'Yesterday';
  }

  if (diffDays < 7) {
    const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    return days[date.getDay()];
  }

  // Older than a week — show date
  const day = date.getDate();
  const months = [
    'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
    'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
  ];
  const month = months[date.getMonth()];
  const year = date.getFullYear();

  if (year === now.getFullYear()) {
    return `${day} ${month}`;
  }

  return `${day} ${month} ${year}`;
}

/**
 * Format a timestamp for the thread detail view.
 * Shows date and time.
 * @param {number|string} timestamp
 * @returns {string}
 */
export function formatMessageDate(timestamp) {
  const date = new Date(parseInt(timestamp, 10));
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const msgDay = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const diffDays = Math.floor((today - msgDay) / (1000 * 60 * 60 * 24));

  const time = formatTime(date);

  if (diffDays === 0) {
    return `Today, ${time}`;
  }
  if (diffDays === 1) {
    return `Yesterday, ${time}`;
  }

  const day = date.getDate();
  const months = [
    'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
    'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
  ];
  const month = months[date.getMonth()];

  return `${day} ${month}, ${time}`;
}

/**
 * Format time as HH:MM AM/PM.
 * @param {Date} date
 * @returns {string}
 */
function formatTime(date) {
  let hours = date.getHours();
  const minutes = date.getMinutes().toString().padStart(2, '0');
  const ampm = hours >= 12 ? 'PM' : 'AM';
  hours = hours % 12 || 12;
  return `${hours}:${minutes} ${ampm}`;
}
