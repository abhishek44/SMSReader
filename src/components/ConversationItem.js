/**
 * ConversationItem — a single row in the conversation list.
 *
 * Displays: sender, last message preview, time, category badge.
 *
 * Props:
 *   conversation: { displayAddress, lastMessage, lastDate, category, messageCount }
 *   onPress: () => void
 */

import React from 'react';
import {View, Text, TouchableOpacity, StyleSheet} from 'react-native';
import Colors from '../theme/colors';
import {formatConversationDate} from '../utils/dateFormatter';
import {getCategoryLabel} from '../utils/classifier';

const CATEGORY_STYLES = {
  transaction: {color: Colors.transaction, bg: Colors.transactionBg},
  personal: {color: Colors.personal, bg: Colors.personalBg},
  promotion: {color: Colors.promotion, bg: Colors.promotionBg},
};

export default function ConversationItem({conversation, onPress}) {
  const {displayAddress, lastMessage, lastDate, category, messageCount} =
    conversation;
  const catStyle = CATEGORY_STYLES[category] || CATEGORY_STYLES.personal;
  const initial = (displayAddress || '?')[0].toUpperCase();

  return (
    <TouchableOpacity
      style={styles.container}
      onPress={onPress}
      activeOpacity={0.6}>
      {/* Avatar */}
      <View style={[styles.avatar, {backgroundColor: catStyle.bg}]}>
        <Text style={[styles.avatarText, {color: catStyle.color}]}>
          {initial}
        </Text>
      </View>

      {/* Content */}
      <View style={styles.content}>
        <View style={styles.topRow}>
          <Text style={styles.sender} numberOfLines={1}>
            {displayAddress}
          </Text>
          <Text style={styles.time}>{formatConversationDate(lastDate)}</Text>
        </View>

        <Text style={styles.preview} numberOfLines={2}>
          {lastMessage}
        </Text>

        <View style={styles.bottomRow}>
          {/* Category badge */}
          <View style={[styles.categoryBadge, {backgroundColor: catStyle.bg}]}>
            <Text style={[styles.categoryText, {color: catStyle.color}]}>
              {getCategoryLabel(category)}
            </Text>
          </View>

          {messageCount > 1 && (
            <Text style={styles.count}>{messageCount} messages</Text>
          )}
        </View>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: Colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: Colors.divider,
  },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  avatarText: {
    fontSize: 18,
    fontWeight: '700',
  },
  content: {
    flex: 1,
    justifyContent: 'center',
  },
  topRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  sender: {
    fontSize: 15,
    fontWeight: '600',
    color: Colors.textPrimary,
    flex: 1,
    marginRight: 8,
  },
  time: {
    fontSize: 12,
    color: Colors.textLight,
  },
  preview: {
    fontSize: 13,
    color: Colors.textSecondary,
    lineHeight: 18,
    marginBottom: 6,
  },
  bottomRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  categoryBadge: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
  },
  categoryText: {
    fontSize: 11,
    fontWeight: '600',
  },
  count: {
    fontSize: 11,
    color: Colors.textLight,
    marginLeft: 8,
  },
});
