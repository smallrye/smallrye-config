package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NameIteratorTest {
    @Test
    void getNextSegment() {
        NameIterator nameIterator = new NameIterator("foo.bar");
        assertEquals("foo", nameIterator.getNextSegment());
        assertEquals("foo", nameIterator.getNextSegment());
    }

    @Test
    void next() {
        NameIterator nameIterator = new NameIterator("foo.bar");
        nameIterator.next();
        assertEquals("bar", nameIterator.getNextSegment());
    }

    @Test
    void getPreviousSegment() {
        NameIterator nameIterator = new NameIterator("foo.bar");
        nameIterator.next();
        assertEquals("foo", nameIterator.getPreviousSegment());
        assertEquals("foo", nameIterator.getPreviousSegment());
    }

    @Test
    void previous() {
        NameIterator nameIterator = new NameIterator("foo.bar");
        nameIterator.next();
        nameIterator.previous();
        assertEquals("foo", nameIterator.getNextSegment());
    }
}
