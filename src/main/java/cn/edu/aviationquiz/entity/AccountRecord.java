package cn.edu.aviationquiz.entity;

/** Persisted account data needed to authenticate and create a session. */
public record AccountRecord(
        String id, String username, String passwordHash, String name, boolean staff) {}
