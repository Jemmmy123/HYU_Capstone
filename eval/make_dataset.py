"""Builds the labeled evaluation set for SensitiveWordDetector.

Output: dataset.tsv with one token per line: sentence_id, token_index, token, label (1 = must not be learned),
category, source. Tokens are split on spaces like the keyboard does; previous words come from the same sentence.
All sensitive values are synthetic (test card numbers, 555 phone numbers, example.com emails).
Run: python3 make_dataset.py   (needs: pip install wordfreq)
"""
import random, csv, string
from wordfreq import top_n_list

rnd = random.Random(20261014)
rows = []  # (sentence tokens, labels, categories, source)

def add(tokens, labels, cats, source):
    rows.append((tokens, labels, cats, source))

def sentence(template, slots, source):
    """template words; '{X}' slots are sensitive values with category X."""
    toks, labels, cats = [], [], []
    for t in template.split():
        if t.startswith('{') and t.endswith('}'):
            cat = t[1:-1]
            toks.append(slots[cat]()); labels.append(1); cats.append(cat)
        else:
            toks.append(t); labels.append(0); cats.append('normal')
    add(toks, labels, cats, source)

# ---------- sensitive value generators ----------
def luhn_complete(prefix, length):
    digits = [int(c) for c in prefix] + [rnd.randrange(10) for _ in range(length - len(prefix) - 1)]
    s = 0
    for i, d in enumerate(reversed(digits)):
        if i % 2 == 0:
            d *= 2
            if d > 9: d -= 9
        s += d
    return ''.join(map(str, digits)) + str((10 - s % 10) % 10)

def card():
    n = luhn_complete(rnd.choice(['4', '51', '52', '37', '6011']), rnd.choice([16, 16, 15]))
    return rnd.choice([n, '-'.join(n[i:i+4] for i in range(0, len(n), 4))])

def phone():
    a, b = rnd.randrange(200, 999), rnd.randrange(1000, 9999)
    return rnd.choice([
        f'010-{rnd.randrange(1000,9999)}-{b}', f'010{rnd.randrange(1000,9999)}{b}',
        f'+1-{a}-555-{b}', f'({a})555-{b}', f'{a}-555-{b}', f'+82-10-{rnd.randrange(1000,9999)}-{b}',
        f'{a}.555.{b}',
    ])

NAMES = ['jamie', 'alex.kim', 'sam_lee', 'jdoe88', 'mina.park', 'chris', 'taylor.s', 'eunji99', 'devon', 'robin.h']
def email():
    name, dom = rnd.choice(NAMES), rnd.choice(['example.com', 'mail.example.org', 'test.co.kr', 'example.net'])
    full = f'{name}@{dom}'
    # the keyboard splits at '.', so the part that can reach learning is often 'name@domain'
    return rnd.choice([full, full.split('.')[0] if '.' not in name else full])

def ssn():
    return f'{rnd.randrange(100,899)}-{rnd.randrange(10,99)}-{rnd.randrange(1000,9999)}'

WORDS = ['summer', 'dragon', 'monkey', 'sunshine', 'princess', 'football', 'shadow', 'master', 'tiger', 'bluefish',
         'coffee', 'pepper', 'ginger', 'winter', 'rocket', 'falcon', 'maple', 'orange', 'hunter', 'silver']
def password():
    w = rnd.choice(WORDS)
    kind = rnd.randrange(6)
    if kind == 0: return w.capitalize() + str(rnd.randrange(1990, 2030))            # Summer2026
    if kind == 1: return w.capitalize() + rnd.choice('!@#$') + str(rnd.randrange(10, 99))  # Tiger!42
    if kind == 2: return w.translate(str.maketrans('aeios', '43105')).capitalize() + '!'  # 5h4d0w!
    if kind == 3: return ''.join(rnd.choice(string.ascii_letters + string.digits) for _ in range(rnd.randrange(10, 16)))
    if kind == 4: return w + str(rnd.randrange(10, 99))                               # dragon42 (weak, 2 classes)
    return w + rnd.choice('!@#') + w[:2] + str(rnd.randrange(100, 999))              # maple#ma123

def account():
    return rnd.choice([
        f'acct{rnd.randrange(10**7, 10**9)}', f'{rnd.randrange(100,999)}-{rnd.randrange(10**5,10**6)}-{rnd.randrange(10,99)}',
        f'KR{rnd.randrange(10**9, 10**10)}', f'ID{rnd.randrange(10**6, 10**7)}',
    ])

def secret_word():  # an ordinary-looking word that is a secret only because of what comes before it
    return rnd.choice(WORDS + ['letmein', 'opensesame', 'qwerty', 'iloveyou', 'starwars', 'trustno1'])

def pin():
    return str(rnd.randrange(1000, 999999))

SLOTS = dict(card=card, phone=phone, email=email, ssn=ssn, password=password, account=account,
             secret=secret_word, pin=pin)

SENSITIVE_TEMPLATES = [
    'my card number is {card} thanks', 'pay with {card} please', 'use card {card} for the order',
    'call me at {phone} tonight', 'my number is {phone} ok', 'text {phone} when you arrive', 'reach me on {phone}',
    'send it to {email} please', 'my email is {email}', 'contact {email} for details',
    'my ssn is {ssn}', 'social security {ssn} on the form',
    'the wifi password is {secret} ok', 'password: {secret}', 'my pin is {pin}', 'pin {pin} for the door',
    'login with {secret} tomorrow', 'username {secret} and password {password}', 'passcode {pin} for the gate',
    'try {password} for the router', 'new one is {password}', 'it is {password} now', 'use {password} to log in',
    'transfer to account {account} today', 'acct {account} is mine', 'my id is {account}',
    'the code is {secret}', 'remember {password} later', 'otp {pin} expires soon',
]
for _ in range(14):
    for t in SENSITIVE_TEMPLATES:
        sentence(t, SLOTS, 'template')

# ---------- normal sentences, including normal uses of trigger words ----------
NORMAL_SENTENCES = '''
I forgot my password again
the password manager is great
reset your password from the settings page
the card game was fun last night
she sent me a birthday card yesterday
pin it on the board please
we need to account for the delay
my account was locked this morning
the login page is down again
please login and check the report
can you pin the message in the group chat
the credit card bill came today
he plays cards every friday
I need a new username for the forum
this account has no money in it
my phone number changed last week
send me an email when you are free
the email was in my spam folder
let's meet at the cafe at 7
the meeting is at 10:30 tomorrow
I bought a new iPhone yesterday
we watched it in 4K on the big TV
the 5G signal is weak here
COVID-19 changed everything in 2020
she works in B2B sales
install the x86_64 version of the app
I downloaded the mp3 file
the update to Android 15 is out
my flight is KE123 on Monday
it costs $19.99 at the store
the store opens at 9am
our office is on the 3rd floor
the 1990s were a great decade
she is a well-known co-founder
I need a T-shirt and jeans
the Wi-Fi in this cafe is slow
he got his Ph.D. last year
the U.S. team won the game
my zip code is 10001
the room number is 1204
it was a state-of-the-art system
we use Office365 at work
I updated to macOS14 yesterday
the hashtag was #throwback2024
the score was 3-2 in the end
the date is 2026-10-14
Python3 is easier than Java
the model name is RTX4090
version 2.0.1 fixed the bug
order number A12345 was shipped
the train leaves from platform 9
see you at 5:45pm
take bus 740 to the station
the recipe needs 250g of flour
the lab result was 0.05
my grandmother turned 90 today
the movie starts at 8 tonight
I walked 12000 steps today
the population is about 51000000
he scored 98 on the test
'''.strip().splitlines()
for s in NORMAL_SENTENCES:
    toks = s.split()
    add(toks, [0] * len(toks), ['normal_tricky'] * len(toks), 'normal_sentence')

# common English words, one per "sentence" (no context), the bulk of everyday typing
for w in top_n_list('en', 3000):
    add([w], [0], ['common_word'], 'wordfreq_top3000')

with open('dataset.tsv', 'w', newline='') as f:
    out = csv.writer(f, delimiter='\t')
    out.writerow(['sentence_id', 'index', 'token', 'label', 'category', 'source'])
    for sid, (toks, labels, cats, source) in enumerate(rows):
        for i, (t, l, c) in enumerate(zip(toks, labels, cats)):
            out.writerow([sid, i, t, l, c, source])
print('sentences', len(rows), 'tokens', sum(len(r[0]) for r in rows),
      'sensitive', sum(sum(r[1]) for r in rows))
