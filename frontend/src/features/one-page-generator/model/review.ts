/** Returns a copy of the list with the item at `index` replaced. */
export function replaceAt<T>(list: T[], index: number, item: T): T[] {
    return list.map((current, i) => (i === index ? item : current));
}

/**
 * Splits textarea text into non-empty lines, capped at `max`, for fields the template holds as
 * one paragraph per line (title lines, checklist bullets).
 */
export function splitLines(text: string, max: number): string[] {
    return text
        .split("\n")
        .filter((line) => line.length > 0)
        .slice(0, max);
}
