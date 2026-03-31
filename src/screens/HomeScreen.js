/**
 * HomeScreen — main screen showing conversation list with category tabs.
 *
 * Features:
 * - Loads SMS on mount
 * - Filter chips: All, Transactions, Personal, Promotions
 * - FlatList of ConversationItems (virtualized for performance)
 * - Empty states for loading / permission denied / no messages
 */

import React, {useState, useEffect, useCallback, useMemo, useLayoutEffect} from 'react';
import {View, FlatList, StyleSheet, StatusBar, TouchableOpacity, Text} from 'react-native';
import Colors from '../theme/colors';
import CategoryTabs from '../components/CategoryTabs';
import ConversationItem from '../components/ConversationItem';
import EmptyState from '../components/EmptyState';
import {fetchConversations} from '../services/SmsService';

const CATEGORIES = [
  {key: 'all', label: 'All'},
  {key: 'transaction', label: 'Transactions'},
  {key: 'personal', label: 'Personal'},
  {key: 'promotion', label: 'Promotions'},
];

export default function HomeScreen({navigation}) {
  const [conversations, setConversations] = useState([]);
  const [activeCategory, setActiveCategory] = useState('all');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Load messages
  const loadMessages = useCallback(async () => {
    setLoading(true);
    setError(null);

    const result = await fetchConversations(500);

    if (result.error) {
      setError(result.error);
    }

    setConversations(result.conversations);
    setLoading(false);
  }, []);

  useEffect(() => {
    loadMessages();
  }, [loadMessages]);

  // Add header right button for settings
  useLayoutEffect(() => {
    navigation.setOptions({
      headerRight: () => (
        <View style={styles.headerButtons}>
          <TouchableOpacity
            style={styles.headerBtn}
            onPress={() => navigation.navigate('ManageAccounts')}>
            <Text style={styles.headerBtnText}>Accounts</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.headerBtn}
            onPress={() => navigation.navigate('ManageCategories')}>
            <Text style={styles.headerBtnText}>Categories</Text>
          </TouchableOpacity>
        </View>
      ),
    });
  }, [navigation]);

  // Filter conversations by active category
  const filteredConversations = useMemo(() => {
    if (activeCategory === 'all') {
      return conversations;
    }
    return conversations.filter(c => c.category === activeCategory);
  }, [conversations, activeCategory]);

  // Build categories with counts
  const categoriesWithCounts = useMemo(() => {
    const counts = {all: conversations.length};
    conversations.forEach(c => {
      counts[c.category] = (counts[c.category] || 0) + 1;
    });

    return CATEGORIES.map(cat => ({
      ...cat,
      count: counts[cat.key] || 0,
    }));
  }, [conversations]);

  // Handlers
  const handleConversationPress = useCallback(
    conversation => {
      navigation.navigate('ThreadDetail', {
        address: conversation.address,
        displayAddress: conversation.displayAddress,
        messages: conversation.messages,
      });
    },
    [navigation],
  );

  const renderItem = useCallback(
    ({item}) => (
      <ConversationItem
        conversation={item}
        onPress={() => handleConversationPress(item)}
      />
    ),
    [handleConversationPress],
  );

  const keyExtractor = useCallback(item => item.address, []);

  // Determine empty state type
  const getEmptyStateType = () => {
    if (loading) return 'loading';
    if (error) return error;
    return 'empty';
  };

  return (
    <View style={styles.container}>
      <StatusBar backgroundColor={Colors.surface} barStyle="dark-content" />

      {/* Category filter tabs */}
      {!loading && conversations.length > 0 && (
        <CategoryTabs
          categories={categoriesWithCounts}
          activeCategory={activeCategory}
          onPress={setActiveCategory}
        />
      )}

      {/* Conversation list or empty state */}
      {loading || error || filteredConversations.length === 0 ? (
        <EmptyState
          type={
            loading
              ? 'loading'
              : error
                ? error
                : filteredConversations.length === 0 &&
                    conversations.length > 0
                  ? 'empty' // No messages in this category
                  : getEmptyStateType()
          }
          onRetry={loadMessages}
        />
      ) : (
        <FlatList
          data={filteredConversations}
          renderItem={renderItem}
          keyExtractor={keyExtractor}
          contentContainerStyle={styles.list}
          showsVerticalScrollIndicator={false}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  list: {
    paddingBottom: 20,
  },
  headerButtons: {
    flexDirection: 'row',
    gap: 4,
  },
  headerBtn: {
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  headerBtnText: {
    color: Colors.primary,
    fontSize: 13,
    fontWeight: '600',
  },
});
