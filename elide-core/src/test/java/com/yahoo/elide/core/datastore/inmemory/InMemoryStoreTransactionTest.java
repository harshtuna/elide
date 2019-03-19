/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.datastore.inmemory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.InPredicate;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import example.Author;
import example.Book;
import example.Editor;
import example.Publisher;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class InMemoryStoreTransactionTest {

    private DataStoreTransaction wrappedTransaction = mock(DataStoreTransaction.class);
    private RequestScope scope = mock(RequestScope.class);
    private InMemoryStoreTransaction inMemoryStoreTransaction = new InMemoryStoreTransaction(wrappedTransaction);
    private EntityDictionary dictionary;
    private Set<Object> books = new HashSet<>();
    private Book book1;
    private Book book2;
    private Book book3;

    @BeforeTest
    public void init() {
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Editor.class);
        dictionary.bindEntity(Publisher.class);

        Editor editor1 = new Editor();
        editor1.setFirstName("Jon");
        editor1.setLastName("Doe");

        Editor editor2 = new Editor();
        editor2.setFirstName("Jane");
        editor2.setLastName("Doe");

        Publisher publisher1 = new Publisher();
        publisher1.setEditor(editor1);

        Publisher publisher2 = new Publisher();
        publisher2.setEditor(editor2);

        book1 = new Book(1,
                "Book 1",
                "Literary Fiction",
                "English",
                System.currentTimeMillis(),
                null,
                publisher1);

        book2 = new Book(2,
                "Book 2",
                "Science Fiction",
                "English",
                System.currentTimeMillis(),
                null,
                publisher1);

        book3 = new Book(3,
                "Book 3",
                "Literary Fiction",
                "English",
                System.currentTimeMillis(),
                null,
                publisher2);

        books.add(book1);
        books.add(book2);
        books.add(book3);
        when(scope.getDictionary()).thenReturn(dictionary);
    }

    @BeforeMethod
    public void resetMocks() {
        reset(wrappedTransaction);
    }

    @Test
    public void testFullFilterPredicatePushDown() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        Optional filterExpressionOp = Optional.of(expression);
        Optional empty = Optional.empty();
        when(wrappedTransaction.supportsFiltering(eq(Book.class),
                any())).thenReturn(DataStoreTransaction.FeatureSupport.FULL);
        when(wrappedTransaction.loadObjects(eq(Book.class), any(), any(), any(), eq(scope))).thenReturn(books);

        Collection<Book> loaded = (Collection<Book>) inMemoryStoreTransaction.loadObjects(
                Book.class,
                filterExpressionOp,
                empty,
                empty,
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(filterExpressionOp),
                eq(empty),
                eq(empty),
                eq(scope));

        Assert.assertEquals(loaded.size(), 3);
        Assert.assertTrue(loaded.contains(book1));
        Assert.assertTrue(loaded.contains(book2));
        Assert.assertTrue(loaded.contains(book3));
    }

    @Test
    public void testTransactionRequiresInMemoryFilter() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        when(scope.isMutatingMultipleEntities()).thenReturn(true);
        when(wrappedTransaction.loadObjects(eq(Book.class), any(), any(), any(), eq(scope))).thenReturn(books);

        Optional filterExpressionOp = Optional.of(expression);
        Optional empty = Optional.empty();

        Collection<Book> loaded = (Collection<Book>) inMemoryStoreTransaction.loadObjects(
                Book.class,
                filterExpressionOp,
                empty,
                empty,
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(empty),
                eq(empty),
                eq(empty),
                eq(scope));

        Assert.assertEquals(loaded.size(), 2);
        Assert.assertTrue(loaded.contains(book1));
        Assert.assertTrue(loaded.contains(book3));
    }

    @Test
    public void testDataStoreRequiresTotalInMemoryFilter() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        when(wrappedTransaction.supportsFiltering(eq(Book.class),
                any())).thenReturn(DataStoreTransaction.FeatureSupport.NONE);
        when(wrappedTransaction.loadObjects(eq(Book.class), any(), any(), any(), eq(scope))).thenReturn(books);

        Optional filterExpressionOp = Optional.of(expression);
        Optional empty = Optional.empty();

        Collection<Book> loaded = (Collection<Book>) inMemoryStoreTransaction.loadObjects(
                Book.class,
                filterExpressionOp,
                empty,
                empty,
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(empty),
                eq(empty),
                eq(empty),
                eq(scope));

        Assert.assertEquals(loaded.size(), 2);
        Assert.assertTrue(loaded.contains(book1));
        Assert.assertTrue(loaded.contains(book3));
    }

    @Test
    public void testDataStoreRequiresPartialInMemoryFilter() {
        FilterExpression expression =
                new InPredicate(new Path(Book.class, dictionary, "genre"), "Literary Fiction");

        when(wrappedTransaction.supportsFiltering(eq(Book.class),
                any())).thenReturn(DataStoreTransaction.FeatureSupport.PARTIAL);
        when(wrappedTransaction.loadObjects(eq(Book.class), any(), any(), any(), eq(scope))).thenReturn(books);

        Optional filterExpressionOp = Optional.of(expression);
        Optional empty = Optional.empty();

        Collection<Book> loaded = (Collection<Book>) inMemoryStoreTransaction.loadObjects(
                Book.class,
                filterExpressionOp,
                empty,
                empty,
                scope);

        verify(wrappedTransaction, times(1)).loadObjects(
                eq(Book.class),
                eq(filterExpressionOp),
                eq(empty),
                eq(empty),
                eq(scope));

        Assert.assertEquals(loaded.size(), 2);
        Assert.assertTrue(loaded.contains(book1));
        Assert.assertTrue(loaded.contains(book3));
    }
}
