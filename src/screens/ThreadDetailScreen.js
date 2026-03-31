/**
 * ThreadDetailScreen — displays all messages in a selected conversation.
 *
 * Route params:
 *   address: string
 *   displayAddress: string
 *   messages: Array of { id, body, date, type, category }
 *
 * Messages are shown in chronological order (oldest at top, newest at bottom).
 * Date headers are inserted when the day changes between messages.
 */

import React, {useMemo} from 'react';
import {View, FlatList, StyleSheet, Text} from 'react-native';
import Colors from '../theme/colors';
import MessageBubble from '../components/MessageBubble';

export default function ThreadDetailScreen({route, navigation}) {
  const {messages, displayAddress} = route.params;

  // Reverse to show chronological order (oldest first, scroll to bottom)
  const chronologicalMessages = useMemo(
    () => [...messages].reverse(),
    [messages],
  );

  // Determine if we should show a date header above a message
  // (when the day changes from the previous message)
  const shouldShowDate = (index) => {
    if (index === 0) return true;
    const current = new Date(chronologicalMessages[index].date);
    const previous = new Date(chronologicalMessages[index - 1].date);
    return (
      current.getDate() !== previous.getDate() ||
      current.getMonth() !== previous.getMonth() ||
      current.getFullYear() !== previous.getFullYear()
    );
  };

  const handleReview = (message) => {
    navigation.navigate('TransactionReview', {
      message,
      senderAddress: displayAddress,
    });
  };

  const renderItem = ({item, index}) => (
    <MessageBubble
      message={item}
      showDate={shouldShowDate(index)}
      onReview={handleReview}
    />
  );

  const keyExtractor = (item) => item.id?.toString() || item.date.toString();

  return (
    <View style={styles.container}>
      {/* Header info */}
      <View style={styles.header}>
        <Text style={styles.headerAddress}>{displayAddress}</Text>
        <Text style={styles.headerCount}>
          {messages.length} message{messages.length !== 1 ? 's' : ''}
        </Text>
      </View>

      {/* Message list */}
      <FlatList
        data={chronologicalMessages}
        renderItem={renderItem}
        keyExtractor={keyExtractor}
        contentContainerStyle={styles.list}
        showsVerticalScrollIndicator={false}
        // Scroll to bottom (newest messages)
        inverted={false}
        onContentSizeChange={(_, contentHeight) => {
          // Auto-scroll handled by initialScrollIndex isn't reliable here,
          // so we let FlatList render normally — user sees oldest first
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  header: {
    backgroundColor: Colors.surface,
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  headerAddress: {
    fontSize: 16,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
  headerCount: {
    fontSize: 12,
    color: Colors.textSecondary,
    marginTop: 2,
  },
  list: {
    paddingVertical: 8,
  },
});
