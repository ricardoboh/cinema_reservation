export const errorText = (e: unknown) => e instanceof Error ? e.message : 'Something went wrong. Please try again.'
