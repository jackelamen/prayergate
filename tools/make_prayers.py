#!/usr/bin/env python3
"""
Builds app/src/main/assets/prayers.json from the lists below.

Run from the project root:   python3 tools/make_prayers.py

Every prayer was written for Prayer Gate, and each one takes up the words of
one Bible passage, which the app shows under the prayer. The passage text
comes from tools/verses.json (made by tools/fetch_verses.py): the World
English Bible and 개역한글 (1961), both free to use. When you change a
reference here, run fetch_verses.py again.

Each entry is: (English prayer, Korean prayer, English reference).
The Korean reference is made from BOOKS_KO below.

Ids are the theme's two-letter prefix plus a number ("tm01"). They are the
key for saved prayers, so never renumber existing prayers: add new ones at
the end of a list and raise EXPECTED_PER_THEME (or remove the check).
"""
import json
import os
import re
import sys

VERSES = {}

EXPECTED_PER_THEME = 10

# Theme key, id prefix
THEMES = [
    ("TEMPTATION", "tm"),
    ("JUDGING", "jd"),
    ("NEIGHBOR", "nb"),
    ("INFLUENCE", "in"),
    ("DISCERNMENT", "ds"),
    ("COMPARISON", "cp"),
    ("WORDS", "wd"),
    ("TIME", "tt"),
]

# English book name -> Korean book name (as Korean Protestant Bibles name them)
BOOKS_KO = {
    "Genesis": "창세기", "1 Samuel": "사무엘상", "Psalm": "시편", "Proverbs": "잠언",
    "Jeremiah": "예레미야",
    "Matthew": "마태복음", "Mark": "마가복음", "Luke": "누가복음", "John": "요한복음",
    "Romans": "로마서", "1 Corinthians": "고린도전서", "2 Corinthians": "고린도후서",
    "Galatians": "갈라디아서", "Ephesians": "에베소서", "Philippians": "빌립보서",
    "Colossians": "골로새서", "1 Thessalonians": "데살로니가전서",
    "1 Timothy": "디모데전서", "2 Timothy": "디모데후서", "Hebrews": "히브리서",
    "James": "야고보서", "1 Peter": "베드로전서", "1 John": "요한일서",
}

PRAYERS = {
    "TEMPTATION": [
        ("Lord, You know what I am about to open and what I tend to look for there. Turn my eyes from worthless things, and give me the strength to scroll past them.",
         "주님, 제가 지금 무엇을 열려고 하는지, 거기서 무엇을 찾곤 하는지 아십니다. 제 눈을 돌이켜 허탄한 것을 보지 말게 하시고, 그것을 지나칠 힘을 주소서.",
         "Psalm 119:37"),
        ("Jesus, You told Your friends to watch and pray. I am doing that now. If something in this app pulls me where I should not go, let me notice it early and close it.",
         "예수님, 제자들에게 깨어 기도하라고 하셨지요. 지금 그렇게 기도합니다. 이 앱에서 무언가가 저를 가지 말아야 할 곳으로 끌어당기면, 일찍 알아차리고 닫게 하소서.",
         "Matthew 26:41"),
        ("Father, You promised a way out with every temptation. Show me the way out today, even if it is only the Close button.",
         "아버지, 시험을 당할 때 피할 길을 내신다고 약속하셨습니다. 오늘도 피할 길을 보여 주소서. 그것이 '닫기' 버튼 하나뿐이라도요.",
         "1 Corinthians 10:13"),
        ("Lord, I will set no vile thing before my eyes. Help me keep that promise for the next few minutes.",
         "주님, 비루한 것을 제 눈앞에 두지 않겠습니다. 앞으로 몇 분 동안 이 다짐을 지키도록 도와주소서.",
         "Psalm 101:3"),
        ("Holy Spirit, help me keep my heart with all diligence while I scroll. What I watch shapes what I want, and my life flows from my heart.",
         "성령님, 제가 화면을 넘기는 동안 무엇보다 제 마음을 지키게 하소서. 보는 것이 원하는 것을 만들고, 생명의 근원이 마음에서 납니다.",
         "Proverbs 4:23"),
        ("Jesus, when I am weak in here, let me come boldly to the throne of grace and find help in this very moment of need.",
         "예수님, 이 안에서 제가 약해질 때, 은혜의 보좌 앞에 담대히 나아가 바로 이 순간 돕는 은혜를 얻게 하소서.",
         "Hebrews 4:16"),
        ("Father, lead me not into temptation, and deliver me from evil. That includes the next video, the next account and the next search.",
         "아버지, 저를 시험에 들게 하지 마시고 악에서 구하소서. 다음 영상, 다음 계정, 다음 검색에서도요.",
         "Matthew 6:13"),
        ("Lord, fill my mind with what is true, pure and lovely, so there is less room for what isn't. Let what I see in here pass that test.",
         "주님, 참되고 정결하고 사랑스러운 것으로 제 생각을 채워 주셔서 그렇지 않은 것이 들어올 자리가 줄어들게 하소서. 이 안에서 보는 것도 그 기준을 통과하게 하소서.",
         "Philippians 4:8"),
        ("Lord, I am tired, and that is when I go looking for water in broken cisterns that hold none. You are the spring of living water. Let me come to You first.",
         "주님, 지금 저는 지쳐 있고, 그럴 때 저는 물을 담지 못하는 터진 웅덩이에서 물을 찾습니다. 주님은 생수의 근원이십니다. 먼저 주님께 나아가게 하소서.",
         "Jeremiah 2:13"),
        ("Father, You tell me to avoid the wrong path, to turn from it and pass on. If I should not be opening this right now, give me the honesty to admit it and the courage to pass on.",
         "아버지, 악한 길을 피하고 돌이켜 떠나가라고 하셨습니다. 지금 이 앱을 열면 안 되는 때라면, 그것을 인정할 정직함과 돌이켜 떠날 용기를 주소서.",
         "Proverbs 4:14–15"),
    ],
    "JUDGING": [
        ("Lord, I am about to see people's faces and their best photos, but You look at the heart. Keep me from judging anyone by what I see in here.",
         "주님, 이제 사람들의 얼굴과 가장 잘 나온 사진들을 보게 됩니다. 사람은 외모를 보지만 주님은 중심을 보십니다. 이 안에서 보는 것으로 누구도 판단하지 않게 하소서.",
         "1 Samuel 16:7"),
        ("Jesus, You asked why I see the speck in my brother's eye and not the beam in my own. Before I react to anyone's post, let me look at my own heart first.",
         "예수님, 형제의 눈 속에 있는 티와 제 눈 속에 있는 들보를 말씀하셨지요. 누군가의 게시물에 반응하기 전에 먼저 제 마음을 돌아보게 하소서.",
         "Matthew 7:3"),
        ("Father, one photo is not a whole life, and one comment is not a whole person. Keep me from judging by appearance before I decide what someone is like.",
         "아버지, 사진 한 장이 한 사람의 삶 전체가 아니고, 댓글 하나가 그 사람 전부가 아닙니다. 누군가를 단정하기 전에 외모로 판단하지 않게 하소서.",
         "John 7:24"),
        ("Lord, when I disagree with someone in here, keep me from turning them into an enemy. Let me argue less and pray for them more.",
         "주님, 이 안에서 누군가와 생각이 다를 때, 그 사람을 적으로 만들지 않게 하소서. 덜 다투고 더 많이 그를 위해 기도하게 하소서.",
         "Matthew 5:44"),
        ("Father, I am quick to reach a verdict about people I have never met. Who am I to judge another? Judgment belongs to You, and You are kinder than I am.",
         "아버지, 저는 만나 본 적도 없는 사람들에 대해 쉽게 결론을 내립니다. 제가 누구이기에 이웃을 판단하겠습니까? 판단은 주님의 것이고, 주님은 저보다 훨씬 자비로우십니다.",
         "James 4:12"),
        ("Lord, if I see a friend caught in a fault today, let me restore them gently and in private instead of taking a screenshot and passing it on. And keep me watchful of myself.",
         "주님, 오늘 친구가 범죄한 일이 드러나는 것을 보게 되면, 캡처해서 퍼뜨리지 않고 온유한 심령으로 조용히 바로잡게 하소서. 그리고 저 자신도 돌아보게 하소서.",
         "Galatians 6:1"),
        ("Jesus, they complained that You welcomed sinners and ate with them. Give me Your eyes for the people this app invites me to laugh at.",
         "예수님, 사람들은 주님이 죄인을 영접하고 함께 음식을 드신다고 원망했습니다. 이 앱이 비웃으라고 보여 주는 사람들을 주님의 눈으로 보게 하소서.",
         "Luke 15:2"),
        ("Lord, forgive me for the times I have judged someone in my head while scrolling. You forgave me. Make me as quick to forgive as I am to criticize.",
         "주님, 화면을 넘기며 속으로 누군가를 판단했던 때를 용서해 주소서. 주님이 저를 용서하신 것처럼, 비판하는 만큼 빨리 용서하는 사람이 되게 하소서.",
         "Colossians 3:13"),
        ("Father, I do not know their story. Keep me from judging or condemning anyone today, and let me give others the benefit of the doubt I would want for myself.",
         "아버지, 저는 그 사람의 사정을 모릅니다. 오늘 누구도 비판하거나 정죄하지 않게 하시고, 제가 받고 싶은 만큼의 너그러움을 다른 사람에게도 베풀게 하소서.",
         "Luke 6:37"),
        ("Lord, the people I am about to see are made in Your image, even the ones I find hardest to like. Let me treat them that way.",
         "주님, 제가 곧 보게 될 사람들은 모두 하나님의 형상대로 지음받았습니다. 좋아하기 가장 어려운 사람까지도요. 그렇게 대하게 하소서.",
         "Genesis 1:27"),
    ],
    "NEIGHBOR": [
        ("Lord, behind every account is a neighbor to love as I love myself. Show me one person in here I can encourage today, and give me the words.",
         "주님, 모든 계정 뒤에는 제 몸과 같이 사랑해야 할 이웃이 있습니다. 오늘 이 안에서 격려할 수 있는 한 사람을 보여 주시고, 할 말도 주소서.",
         "Mark 12:31"),
        ("Father, when a friend shares good news, let me rejoice with them without a trace of envy. When they share bad news, let me call or write to them myself.",
         "아버지, 친구가 기쁜 소식을 올리면 시기 없이 함께 기뻐하게 하소서. 슬픈 소식을 올리면 제가 직접 전화하거나 메시지를 보내게 하소서.",
         "Romans 12:15"),
        ("Jesus, a like is love in word only. Make me willing to love in deed and in truth: a message, a call, a visit.",
         "예수님, '좋아요'는 말과 혀로만 하는 사랑입니다. 메시지, 전화, 방문처럼 행함과 진실함으로 사랑하게 하소서.",
         "1 John 3:18"),
        ("Lord, someone I follow may be struggling and hiding it well. Help me notice, and help me carry their burden by caring enough to ask.",
         "주님, 제가 팔로우하는 누군가가 힘든데 그것을 잘 숨기고 있을지도 모릅니다. 알아차리게 하시고, 물어볼 만큼 마음 써서 그의 짐을 함께 지게 하소서.",
         "Galatians 6:2"),
        ("Father, let me look not only to my own interests in here, but also to the interests of others.",
         "아버지, 이 안에서도 제 일만 돌아보지 않고 다른 사람의 일도 돌아보게 하소서.",
         "Philippians 2:4"),
        ("Lord, the person I want to argue with is also my neighbor. Teach me to love them when they are wrong, and when I am, and as far as it depends on me, to be at peace with them.",
         "주님, 제가 논쟁하고 싶은 그 사람도 제 이웃입니다. 그가 틀렸을 때도, 제가 틀렸을 때도 그를 사랑하게 하시고, 할 수 있는 한 그와 화평하게 하소서.",
         "Romans 12:18"),
        ("Jesus, the Samaritan saw the man everyone else walked past, and was moved with compassion. Let me see someone in here today, and stop.",
         "예수님, 사마리아인은 모두가 지나친 그 사람을 보고 불쌍히 여겼습니다. 오늘 이 안에서 저도 누군가를 보고 멈추게 하소서.",
         "Luke 10:33"),
        ("Father, the people in my feed are people You love. Bring a few of their names to mind when I pray tonight.",
         "아버지, 제 피드에 있는 사람들은 주님이 사랑하시는 사람들입니다. 오늘 밤 기도할 때 그중 몇 사람의 이름을 떠올리게 하소서.",
         "1 Timothy 2:1"),
        ("Lord, let my words in here build people up, the way a good friend's words do.",
         "주님, 이 안에서 제가 하는 말이 좋은 친구의 말처럼 사람들을 세워 주게 하소서.",
         "1 Thessalonians 5:11"),
        ("Lord, someone lonely may be scrolling right now too. A friend loves at all times. If I can be that friend to them today, use me.",
         "주님, 지금 외로운 누군가도 화면을 넘기고 있을지 모릅니다. 친구는 사랑이 끊이지 않는다고 하셨습니다. 오늘 제가 그에게 그런 친구가 될 수 있다면 저를 사용하소서.",
         "Proverbs 17:17"),
    ],
    "INFLUENCE": [
        ("Jesus, You told me to let my light shine so that people glorify the Father. Whatever I post today, let it point people to Him more than to me.",
         "예수님, 빛을 사람 앞에 비추어 하늘에 계신 아버지께 영광을 돌리게 하라고 하셨습니다. 오늘 제가 무엇을 올리든, 사람들이 저보다 아버지를 보게 하소서.",
         "Matthew 5:16"),
        ("Lord, am I trying to please people or You? I care too much about how many people see what I share. Let me care more about whether it pleases You.",
         "주님, 저는 사람의 기쁨을 구합니까, 주님의 기쁨을 구합니까? 제가 올린 것을 몇 명이 보는지에 너무 마음을 씁니다. 그것이 주님을 기쁘시게 하는지에 더 마음 쓰게 하소서.",
         "Galatians 1:10"),
        ("Father, someone younger in faith may be watching what I like, share and laugh at. Keep me from becoming their stumbling block.",
         "아버지, 믿음이 어린 누군가가 제가 무엇에 '좋아요'를 누르고, 공유하고, 웃는지를 보고 있을 수 있습니다. 제가 그에게 걸림돌이 되지 않게 하소서.",
         "1 Corinthians 8:9"),
        ("Lord, if someone asks about the hope I have, let me be ready to answer with humility and respect, even in a comment section.",
         "주님, 누군가 제 속에 있는 소망에 대해 묻는다면, 댓글에서라도 온유와 두려움으로 대답할 준비가 되어 있게 하소서.",
         "1 Peter 3:15"),
        ("Jesus, You warned against doing good to be seen. Before I post, show me why I want to.",
         "예수님, 사람에게 보이려고 의를 행하지 말라고 하셨습니다. 게시물을 올리기 전에, 제가 왜 그것을 올리려 하는지 보여 주소서.",
         "Matthew 6:1"),
        ("Lord, let my words in here be full of grace and seasoned with salt, so I know how to answer each person.",
         "주님, 이 안에서 제 말이 은혜롭고 소금으로 맛을 냄 같게 하셔서, 각 사람에게 어떻게 대답할지 알게 하소서.",
         "Colossians 4:6"),
        ("Father, whatever I do here, in word or deed, let me do it in the name of Jesus.",
         "아버지, 이 안에서 말을 하든지 일을 하든지 모든 것을 예수님의 이름으로 하게 하소서.",
         "Colossians 3:17"),
        ("Lord, I am Your ambassador online too. Let nothing I share today embarrass the One who sent me.",
         "주님, 온라인에서도 저는 주님의 대사입니다. 오늘 제가 공유하는 어떤 것도 저를 보내신 분께 부끄럽지 않게 하소서.",
         "2 Corinthians 5:20"),
        ("Father, speaking the truth in love gets fewer likes than being clever and cruel. Let me speak the truth in love anyway.",
         "아버지, 사랑 안에서 참된 것을 말하면 영리하고 날카로운 말보다 '좋아요'를 덜 받습니다. 그래도 사랑 안에서 참된 것을 말하게 하소서.",
         "Ephesians 4:15"),
        ("Lord, let me set an example in speech, conduct, love, faith and purity, starting with what I post today.",
         "주님, 말과 행실과 사랑과 믿음과 정절에서 본이 되게 하소서. 오늘 제가 올리는 것부터요.",
         "1 Timothy 4:12"),
    ],
    "DISCERNMENT": [
        ("Lord, not everything I read in here is true, and not everything that makes me angry deserves my anger. Give me wisdom to tell the difference.",
         "주님, 이 안에서 읽는 것이 모두 참은 아니고, 저를 화나게 하는 것이 모두 분노할 만한 일도 아닙니다. 그 차이를 분별할 지혜를 주소서.",
         "James 1:5"),
        ("Father, teach me to test everything and hold on to what is good, including the next headline I see.",
         "아버지, 범사에 헤아려 좋은 것을 취하게 하소서. 제가 보게 될 다음 기사 제목까지도요.",
         "1 Thessalonians 5:21"),
        ("Lord, the first story I hear always sounds right. Remind me to wait for the other side before I share or react.",
         "주님, 먼저 들은 이야기는 언제나 옳아 보입니다. 공유하거나 반응하기 전에 다른 쪽 이야기를 기다리게 하소서.",
         "Proverbs 18:17"),
        ("Holy Spirit, some voices in here sound Christian but lead away from Christ. Help me test what I hear against Your word.",
         "성령님, 이 안의 어떤 목소리는 신앙처럼 들리지만 그리스도에게서 멀어지게 합니다. 듣는 것을 말씀으로 분별하게 하소서.",
         "1 John 4:1"),
        ("Lord, I do not want this feed to shape my mind. Renew my mind so I can see what Your will is.",
         "주님, 이 피드가 제 생각의 틀을 정하지 않기를 원합니다. 제 마음을 새롭게 하셔서 주님의 뜻이 무엇인지 분별하게 하소서.",
         "Romans 12:2"),
        ("Father, the simple believe every word. Make me careful, so I do not pass along something false.",
         "아버지, 어리석은 자는 온갖 말을 믿습니다. 저를 신중하게 하셔서 거짓을 퍼뜨리지 않게 하소서.",
         "Proverbs 14:15"),
        ("Lord, keep me from being tossed back and forth by every opinion that trends today. Root me in what does not change.",
         "주님, 오늘 유행하는 온갖 의견의 풍조에 밀려 요동치 않게 하소서. 변하지 않는 것에 저를 뿌리내리게 하소서.",
         "Ephesians 4:14"),
        ("Father, let my love grow in knowledge and discernment, so I can choose what is excellent instead of what is loudest.",
         "아버지, 제 사랑이 지식과 모든 총명으로 더욱 풍성해져서, 가장 시끄러운 것이 아닌 지극히 선한 것을 분별하게 하소서.",
         "Philippians 1:9–10"),
        ("Lord, I trust You, not my own understanding, to make my paths straight. Show me if this app is steering me more than You are.",
         "주님, 제 명철을 의지하지 않고 주님을 의뢰합니다. 주님이 제 길을 지도해 주소서. 주님보다 이 앱이 저를 이끌고 있다면 알게 하소서.",
         "Proverbs 3:5–6"),
        ("Lord, the truth sets me free. Let me love the truth more than the stories I want to be true.",
         "주님, 진리가 저를 자유롭게 합니다. 사실이기를 바라는 이야기보다 진리를 더 사랑하게 하소서.",
         "John 8:32"),
    ],
    "COMPARISON": [
        ("Lord, I am about to see the best moments of other people's lives. Comparing myself with others is without understanding. Keep me from doing it.",
         "주님, 이제 다른 사람들 삶의 가장 좋은 순간들을 보게 됩니다. 자기를 남과 비교하는 것은 지혜가 없는 일입니다. 그렇게 하지 않게 지켜 주소서.",
         "2 Corinthians 10:12"),
        ("Jesus, when Peter asked about John, You said, \"What is that to you? You follow me.\" Say it to me today when I look at someone else's life.",
         "예수님, 베드로가 요한에 대해 물었을 때 \"네게 무슨 상관이냐 너는 나를 따르라\" 하셨습니다. 다른 사람의 삶을 바라볼 때 오늘 제게도 그렇게 말씀해 주소서.",
         "John 21:22"),
        ("Father, Your word says envy rots the bones. Give me a heart at peace with what You have given me.",
         "아버지, 시기는 뼈를 썩게 한다고 말씀하셨습니다. 주님이 주신 것에 평안한 마음을 주소서.",
         "Proverbs 14:30"),
        ("Lord, teach me the secret of being content, whether my life looks impressive online or not.",
         "주님, 제 삶이 온라인에서 근사해 보이든 그렇지 않든 자족하는 비결을 배우게 하소서.",
         "Philippians 4:12"),
        ("Father, You made me fearfully and wonderfully. Let no filter or follower count tell me otherwise.",
         "아버지, 주님은 저를 신묘막측하게 지으셨습니다. 어떤 필터나 팔로워 수도 이와 다르게 말하지 못하게 하소서.",
         "Psalm 139:14"),
        ("Lord, my feet almost slipped when I envied the people who seem to have it all. Turn my eyes back to You before I scroll myself into bitterness.",
         "주님, 형통한 사람들을 보고 질시하다가 제 발이 미끄러질 뻔했습니다. 화면을 넘기다 마음이 쓰라려지기 전에 제 눈을 주님께 돌려 주소서.",
         "Psalm 73:2–3"),
        ("Jesus, You will never leave me or forsake me. When I see what others have, let me be content with what I have.",
         "예수님, 주님은 결코 저를 버리지 않고 떠나지 않으십니다. 다른 사람이 가진 것을 볼 때, 제게 있는 것을 족한 줄로 알게 하소서.",
         "Hebrews 13:5"),
        ("Lord, let me test my own work and be glad in it, without needing someone else to look worse.",
         "주님, 다른 사람이 못해 보여야 기뻐하지 않고, 제 일을 스스로 살피며 기뻐하게 하소서.",
         "Galatians 6:4"),
        ("Father, godliness with contentment is great gain. Make me rich in that, whatever my feed tells me I lack.",
         "아버지, 자족하는 마음이 있는 경건은 큰 이익입니다. 제 피드가 제게 무엇이 부족하다고 말하든, 그 이익에 부요하게 하소서.",
         "1 Timothy 6:6"),
        ("Lord, people post the trip, the ring and the promotion, and almost nobody posts the waiting. Remind me that You are at work in my waiting too.",
         "주님, 사람들은 여행과 반지와 승진은 올리지만 기다림은 거의 올리지 않습니다. 제가 기다리는 동안에도 주님이 일하고 계심을 기억하게 하소서.",
         "Psalm 27:14"),
    ],
    "WORDS": [
        ("Lord, set a watch before my mouth, and before my thumbs too. Keep the door of my lips today.",
         "주님, 제 입 앞에 파수꾼을 세우시고, 제 손가락 앞에도 세워 주소서. 오늘 제 입술의 문을 지켜 주소서.",
         "Psalm 141:3"),
        ("Father, make me quick to listen, slow to speak and slow to anger, especially in the comments.",
         "아버지, 듣기는 속히 하고 말하기는 더디 하며 성내기도 더디 하게 하소서. 특히 댓글에서요.",
         "James 1:19"),
        ("Lord, let only words that build up come from me, the kind that give grace to whoever reads them.",
         "주님, 덕을 세우는 말만 제게서 나오게 하셔서 읽는 사람에게 은혜가 되게 하소서.",
         "Ephesians 4:29"),
        ("Jesus, You said I will give account for every idle word. Remind me that typed words count.",
         "예수님, 무익한 말마다 심판 날에 심문을 받으리라 하셨습니다. 글로 쓴 말도 마찬가지임을 기억하게 하소서.",
         "Matthew 12:36"),
        ("Father, a gentle answer turns away wrath. If someone provokes me in here, let me answer gently or not at all.",
         "아버지, 유순한 대답은 분노를 쉬게 합니다. 누군가 저를 자극하면 부드럽게 대답하거나 아예 대답하지 않게 하소서.",
         "Proverbs 15:1"),
        ("Lord, keep me from being hasty in my words. When I want to post something sharp, give me the patience to wait and the humility to delete it.",
         "주님, 언어에 조급한 사람이 되지 않게 하소서. 날카로운 말을 올리고 싶을 때 기다릴 인내와 그것을 지울 겸손을 주소서.",
         "Proverbs 29:20"),
        ("Father, the tongue has the power of life and death. Let my words give life to someone today.",
         "아버지, 죽고 사는 것이 혀의 권세에 달렸습니다. 오늘 제 말이 누군가를 살리게 하소서.",
         "Proverbs 18:21"),
        ("Lord, let the words I type and the thoughts behind them be pleasing in Your sight.",
         "주님, 제가 쓰는 말과 그 뒤에 있는 생각이 주님 앞에 열납되게 하소서.",
         "Psalm 19:14"),
        ("Father, where words are many, sin is not far away. Teach me to restrain my lips, and to know when to say nothing.",
         "아버지, 말이 많으면 허물을 면하기 어렵습니다. 제 입술을 제어하게 하시고, 아무 말도 하지 않아야 할 때를 가르쳐 주소서.",
         "Proverbs 10:19"),
        ("Lord, whispered gossip separates close friends. Keep me from it, including the kind that arrives as a screenshot in a group chat.",
         "주님, 말쟁이는 친한 벗을 이간합니다. 단체 채팅방에 올라오는 캡처 한 장 같은 험담에서도 저를 지켜 주소서.",
         "Proverbs 16:28"),
    ],
    "TIME": [
        ("Lord, teach me to number my days, so I spend this one with a wise heart.",
         "주님, 우리에게 우리 날 계수함을 가르치사 오늘 하루를 지혜로운 마음으로 보내게 하소서.",
         "Psalm 90:12"),
        ("Father, You tell me to walk wisely and redeem the time. I mean to open this for a minute. Help me keep that minute a minute.",
         "아버지, 지혜 있는 자같이 행하여 세월을 아끼라고 하셨습니다. 잠깐만 보려고 이 앱을 엽니다. 그 잠깐이 정말 잠깐으로 끝나게 하소서.",
         "Ephesians 5:15–16"),
        ("Lord, You told me to be still and know that You are God. Let me be still with You for a moment before I open this.",
         "주님, 가만히 있어 하나님 되심을 알라 하셨습니다. 이것을 열기 전에 잠시 주님 앞에 가만히 있게 하소서.",
         "Psalm 46:10"),
        ("Jesus, Martha was busy with many things and Mary chose the one thing needed. Show me the one thing needed right now.",
         "예수님, 마르다는 많은 일로 분주했고 마리아는 필요한 한 가지를 택했습니다. 지금 제게 필요한 한 가지를 보여 주소서.",
         "Luke 10:41–42"),
        ("Lord, what I call boredom is often restlessness, and only You give rest. Let me come to You first.",
         "주님, 제가 지루함이라 부르는 것은 대개 마음의 불안입니다. 쉼은 주님만 주십니다. 먼저 주님께 나아가게 하소서.",
         "Matthew 11:28"),
        ("Father, You give sleep to those You love. If it is late, give me the sense to put this down and rest.",
         "아버지, 주님은 사랑하시는 자에게 잠을 주십니다. 밤이 늦었다면 이것을 내려놓고 쉴 지혜를 주소서.",
         "Psalm 127:2"),
        ("Jesus, You rose early and went to a quiet place to pray. Let my first look in the morning be toward You before this screen.",
         "예수님, 주님은 새벽 일찍 한적한 곳에 가서 기도하셨습니다. 아침에 제 첫 시선이 이 화면보다 먼저 주님을 향하게 하소서.",
         "Mark 1:35"),
        ("Lord, all things are lawful for me, but not all things are helpful. Let me not be brought under the power of anything in this phone.",
         "주님, 모든 것이 가하나 다 유익한 것은 아닙니다. 이 휴대폰 안의 어떤 것에도 제가 제재를 받지 않게 하소서.",
         "1 Corinthians 6:12"),
        ("Father, You tell me to honor others above myself. The people near me deserve more of my attention than the people in this app. Help me look up.",
         "아버지, 서로 먼저 존경하라고 하셨습니다. 이 앱 속 사람들보다 제 곁에 있는 사람들이 제 관심을 더 받아야 합니다. 고개를 들게 하소서.",
         "Romans 12:10"),
        ("Lord, set my mind on things above. Let me give this app a few minutes, and give the rest of my attention to You and the people You have given me.",
         "주님, 위의 것을 생각하게 하소서. 이 앱에는 몇 분만 쓰고, 나머지 마음은 주님과 주님이 주신 사람들에게 쓰게 하소서.",
         "Colossians 3:2"),
    ],
}

REF_PATTERN = re.compile(r"^(?P<book>(?:\d )?[A-Z][a-z]+(?: [A-Z][a-z]+)?) (?P<cv>\d+:\d+(?:–\d+)?)$")


def parse_reference(ref: str):
    """'Psalm 23:1–3' -> ('Psalm', 23, 1, 3)."""
    m = REF_PATTERN.match(ref)
    if not m:
        raise SystemExit(f"Bad reference format: {ref!r} (expected e.g. 'Psalm 23:1–3')")
    chapter, verses = m.group("cv").split(":")
    first, _, last = verses.partition("–")
    return m.group("book"), int(chapter), int(first), int(last or first)


def korean_reference(ref: str) -> str:
    m = REF_PATTERN.match(ref)
    if not m:
        raise SystemExit(f"Bad reference format: {ref!r} (expected e.g. 'Psalm 23:1–3')")
    book = m.group("book")
    if book not in BOOKS_KO:
        raise SystemExit(f"No Korean name for book {book!r}: add it to BOOKS_KO")
    # Korean Bibles write ranges with a tilde: 시편 23:1~3
    return f"{BOOKS_KO[book]} {m.group('cv').replace('–', '~')}"


def typographic(text: str) -> str:
    """Straight quotes and apostrophes become the curly ones a printed book uses."""
    text = re.sub(r'(^|[\s(])"', "\\1\u201c", text)   # opening double quote
    text = text.replace('"', "\u201d")                 # closing double quote
    return text.replace("'", "\u2019")                 # apostrophe


def load_verses():
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "verses.json")
    if not os.path.exists(path):
        return {}
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def main():
    global VERSES
    VERSES = load_verses()
    errors = []
    out = []
    seen_text = set()
    for theme, prefix in THEMES:
        items = PRAYERS[theme]
        if len(items) != EXPECTED_PER_THEME:
            errors.append(f"{theme}: {len(items)} prayers, expected {EXPECTED_PER_THEME}")
        for i, (en, ko, ref) in enumerate(items, start=1):
            pid = f"{prefix}{i:02d}"
            for label, text in (("English", en), ("Korean", ko)):
                if not text.strip():
                    errors.append(f"{pid}: empty {label} text")
                if "—" in text:
                    errors.append(f"{pid}: {label} text has an em dash")
            words = len(en.split())
            if not 12 <= words <= 45:
                errors.append(f"{pid}: {words} English words (keep prayers between 12 and 45)")
            if en in seen_text:
                errors.append(f"{pid}: duplicate English text")
            seen_text.add(en)
            verse = VERSES.get(ref)
            if verse is None:
                errors.append(f"{pid}: no text for {ref} in tools/verses.json (run tools/fetch_verses.py)")
                verse = {"en": "", "ko": ""}
            out.append({
                "id": pid,
                "theme": theme,
                "text": typographic(en),
                "textKo": ko,
                "reference": ref,
                "referenceKo": korean_reference(ref),
                "verse": verse["en"],
                "verseKo": verse["ko"],
            })
    if errors:
        print("\n".join(errors))
        sys.exit(1)

    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    path = os.path.join(root, "app", "src", "main", "assets", "prayers.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print(f"Wrote {len(out)} prayers in {len(THEMES)} themes")


if __name__ == "__main__":
    main()
