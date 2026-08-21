// Vitest setup, registered from vite.config.ts.
//
// Two things have to happen here rather than per test file:
//
//  1. `cleanup` after every test. Testing Library registers this itself only
//     when Vitest runs with `globals: true`. This project imports `describe`,
//     `it`, and `expect` explicitly instead, so nothing unmounts the previous
//     render and a second test in the same file sees both DOM trees — every
//     query then fails with "found multiple elements".
//  2. jest-dom's matchers, so assertions can read `toBeVisible()` and
//     `toHaveAttribute()` rather than poking at DOM properties by hand.
import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

afterEach(() => {
    cleanup();
});
