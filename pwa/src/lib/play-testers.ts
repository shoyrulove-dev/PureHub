export const PLAY_TESTER_GROUP_URL = 'https://groups.google.com/g/purehub-testers'
export const PLAY_TESTER_INVITE_DISMISSED_KEY = 'purehub-play-tester-invite-v1'

export function openPlayTesterGroup() {
  window.open(PLAY_TESTER_GROUP_URL, '_blank', 'noopener,noreferrer')
}
