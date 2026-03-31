/**
 * MessageBubble — displays a single SMS message in the thread detail view.
 *
 * Props:
 *   message: { id, body, date, type, category }
 *   showDate: boolean — whether to show the date header above this message
 *   onReview: (message) => void — callback when user taps "Review" (transaction only)
 */

import React from 'react';
import {View, Text, TouchableOpacity, StyleSheet} from 'react-native';
import Colors from '../theme/colors';
import {formatMessageDate} from '../utils/dateFormatter';
import {getCategoryLabel} from '../utils/classifier';

const CATEGORY_COLORS = {
  transaction: Colors.transaction,
  personal: Colors.personal,
  promotion: Colors.promotion,
};

export default function MessageBubble({message, showDate, onReview}) {
  const {body, date, category} = message;
  const catColor = CATEGORY_COLORS[category] || Colors.personal;
  const isTransaction = category === 'transaction';

  return (
    <View style={styles.wrapper}>
      {showDate && (
        <Text style={styles.dateHeader}>{formatMessageDate(date)}</Text>
      )}

      <View style={styles.bubble}>
        <Text style={styles.body}>{body}</Text>

        <View style={styles.footer}>
          <View style={[styles.dot, {backgroundColor: catColor}]} />
          <Text style={[styles.categoryLabel, {color: catColor}]}>
            {getCategoryLabel(category)}
          </Text>
          <Text style={styles.time}>{formatMessageDate(date)}</Text>
        </View>

        {/* Review button — only for transaction messages */}
        {isTransaction && onReview && (
          <TouchableOpacity
            style={styles.reviewButton}
            onPress={() => onReview(message)}
            activeOpacity={0.7}>
            <Text style={styles.reviewButtonText}>Review Transaction</Text>
          </TouchableOpacity>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    paddingHorizontal: 16,
    marginBottom: 8,
  },
  dateHeader: {
    textAlign: 'center',
    fontSize: 12,
    color: Colors.textLight,
    marginVertical: 12,
    fontWeight: '500',
  },
  bubble: {
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: 12,
    maxWidth: '90%',
    // Subtle shadow
    shadowColor: Colors.shadow,
    shadowOffset: {width: 0, height: 1},
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 1,
  },
  body: {
    fontSize: 14,
    color: Colors.textPrimary,
    lineHeight: 20,
  },
  footer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 8,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    marginRight: 4,
  },
  categoryLabel: {
    fontSize: 11,
    fontWeight: '600',
    marginRight: 8,
  },
  time: {
    fontSize: 11,
    color: Colors.textLight,
    marginLeft: 'auto',
  },
  reviewButton: {
    marginTop: 10,
    backgroundColor: Colors.transactionBg,
    borderRadius: 6,
    paddingVertical: 7,
    alignItems: 'center',
  },
  reviewButtonText: {
    color: Colors.transaction,
    fontSize: 12,
    fontWeight: '700',
  },
});
