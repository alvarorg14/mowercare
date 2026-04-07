# ADR 0003: Expo / React Native for the mobile client

## Status

Accepted

## Context

MowerCare targets **iOS and Android** field users with push notifications and offline-tolerant UX. Options included **native** (Swift/Kotlin), **Flutter**, and **React Native** (bare or **Expo**).

## Decision

Use **Expo** with **Expo Router**, **TypeScript**, **React Native**, and **expo-notifications** for push registration.

## Consequences

**Positive**

- One codebase for two platforms; faster iteration for an MVP.
- **Expo** tooling (CLI, OTA workflows when configured) and managed native modules for common needs.
- Rich React ecosystem: **TanStack Query**, **React Hook Form**, **Zod**, **React Native Paper**.

**Negative / trade-offs**

- Some native edge cases may require config plugins or eject-style work.
- Performance tuning follows React Native norms (lists, re-renders).

## Related

- [../mobile-architecture.md](../mobile-architecture.md)  
