package com.prslc.zhiflow.ui.preview

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.prslc.zhiflow.data.dto.AnswerDto
import com.prslc.zhiflow.data.dto.ChatMessageDto
import com.prslc.zhiflow.data.dto.ChatPeerDto
import com.prslc.zhiflow.data.dto.CollectionItemDto
import com.prslc.zhiflow.data.dto.ColumnDto
import com.prslc.zhiflow.data.dto.ColumnItemDto
import com.prslc.zhiflow.data.dto.CommentDto
import com.prslc.zhiflow.data.dto.FeedAuthorNote
import com.prslc.zhiflow.data.dto.FeedDto
import com.prslc.zhiflow.data.dto.FeedReason
import com.prslc.zhiflow.data.dto.FeedbackAction
import com.prslc.zhiflow.data.dto.HotQueryDto
import com.prslc.zhiflow.data.dto.ImageData
import com.prslc.zhiflow.data.dto.MessageThreadDto
import com.prslc.zhiflow.data.dto.MomentContentType
import com.prslc.zhiflow.data.dto.MomentDto
import com.prslc.zhiflow.data.dto.NotificationCategoryDto
import com.prslc.zhiflow.data.dto.NotificationItemDto
import com.prslc.zhiflow.data.dto.PersonDto
import com.prslc.zhiflow.data.dto.ReadHistoryDto
import com.prslc.zhiflow.data.dto.SearchItemDto
import com.prslc.zhiflow.data.dto.SuggestedQueryDto
import com.prslc.zhiflow.data.model.comment.Author
import com.prslc.zhiflow.data.model.comment.CommentAuthor
import com.prslc.zhiflow.data.model.comment.CommentContent
import com.prslc.zhiflow.data.model.content.AnswerAuthor
import com.prslc.zhiflow.data.model.content.Formula
import com.prslc.zhiflow.data.model.content.QuestionDetail
import com.prslc.zhiflow.data.model.content.Topic
import com.prslc.zhiflow.data.model.content.ZhihuImage
import com.prslc.zhiflow.data.model.moment.MediaImage
import com.prslc.zhiflow.data.model.user.ZhihuUser
import com.prslc.zhiflow.data.remote.parser.model.CodeToken
import com.prslc.zhiflow.data.remote.parser.model.CodeTokenKind
import com.prslc.zhiflow.data.remote.parser.model.InlineFormulaMeta
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement
import com.prslc.zhiflow.ui.component.common.ProfileStat

/**
 * What the previews are drawn with.
 *
 * One set of samples for the whole app rather than one per file: a row previewed on its own page
 * and the same row previewed inside a list should be showing the same reader, and two files
 * inventing two answers to "what does a feed card hold" is how a preview stops being evidence.
 *
 * The prose is English, like the rest of the code, and the numbers are shaped rather than round --
 * a count of 1 reads the same whether the layout handles thousands or not. The picture addresses
 * are the API's own host so that a preview with a network shows a picture; with none it shows
 * whatever the app draws when a load fails, which is a state worth looking at too.
 *
 * Everything here is `internal val`, and nothing in the app refers to it: a preview is the only
 * caller, so R8 takes the lot out of a release build.
 */

private const val AVATAR_URL = "https://picx.zhimg.com/v2-8c1f0a2b3d4e5f60718293a4b5c6d7e8_l.jpg"

private const val PHOTO_URL = "https://picx.zhimg.com/v2-4d5e6f708192a3b4c5d6e7f8091a2b3c_r.jpg"

private const val WIDE_URL = "https://picx.zhimg.com/v2-9a8b7c6d5e4f30211f2e3d4c5b6a7988_r.jpg"

private val SampleImage = ImageData(url = PHOTO_URL, width = 1200, height = 800)

private val SampleWideImage = ImageData(url = WIDE_URL, width = 1600, height = 900)

private val SampleZhihuImage = ZhihuImage(
    urls = listOf(PHOTO_URL),
    width = 1200,
    height = 800,
    description = "A diagram of a parse tree",
    isGif = false,
)

// Feed

val sampleFeed = FeedDto(
    id = "1184662837846016000",
    type = "answer",
    title = "How does a parser turn segments into elements?",
    reason = FeedReason(text = "Because you read about parsing", iconUrl = null),
    authorName = "Ada Lovelace",
    authorAvatar = AVATAR_URL,
    authorNote = FeedAuthorNote(
        text = "Followed by Grace Hopper",
        kind = "followed_by",
        count = 1,
    ),
    excerpt = "A segment arrives as a tree and leaves as a flat list of things a composable can " +
        "draw. The marks on the leaves are what decide which of them a paragraph becomes.",
    images = listOf(SampleImage, SampleWideImage),
    voteCount = 1284,
    commentCount = 96,
)

/** The same card with everything the server may leave out left out. */
val sampleBareFeed = FeedDto(
    id = "1184662837846016001",
    type = "article",
    title = "Notes on the segment tree",
    reason = null,
    authorName = "Grace Hopper",
    authorAvatar = null,
    authorNote = null,
    excerpt = "Short.",
    images = emptyList(),
    voteCount = 0,
    commentCount = 0,
)

val sampleAnswer = AnswerDto(
    id = "734558056",
    authorName = "Grace Hopper",
    authorAvatar = AVATAR_URL,
    excerpt = "The interesting part is not the tree. It is that the leaves agree on what a run of " +
        "text means before anything is drawn.",
    images = listOf(SampleWideImage),
    voteCount = 4210,
    commentCount = 233,
)

// People

val samplePerson = PersonDto(
    id = "8f2a1b3c4d5e6f70",
    urlToken = "ada-lovelace",
    name = "Ada Lovelace",
    headline = "Writes about parsers and the machines that read them",
    avatarUrl = AVATAR_URL,
    isFollowing = false,
)

val sampleFollowedPerson = PersonDto(
    id = "1a2b3c4d5e6f7081",
    urlToken = "grace-hopper",
    name = "Grace Hopper",
    headline = "Compilers, and why the tape does not care how you indent",
    avatarUrl = AVATAR_URL,
    isFollowing = true,
)

val sampleUser = ZhihuUser(
    id = "8f2a1b3c4d5e6f70",
    urlToken = "ada-lovelace",
    name = "Ada Lovelace",
    headline = "Writes about parsers and the machines that read them",
    userDescription = "Notes on turning text into something a program can walk.",
    avatar = AVATAR_URL,
    coverUrl = PHOTO_URL,
    followerCount = 12400,
    followingCount = 321,
    voteupCount = 98211,
    favoriteCount = 2100,
    pinsCount = 12,
    answerCount = 128,
    isFollowing = false,
    isFollowed = true,
)

val sampleProfileStats = listOf(
    ProfileStat(label = "Answers", count = 128),
    ProfileStat(label = "Followers", count = 12400, onClick = {}),
)

// Search

val sampleSearchResult = SearchItemDto.Content(
    id = "734558056",
    type = "answer",
    title = buildAnnotatedString {
        append("What a ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("parser") }
        append(" does with a segment")
    },
    excerpt = buildAnnotatedString {
        append("The ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("parser") }
        append(
            " walks the tree depth first and emits one element per leaf, which is why the " +
                "order of the marks on the text is the order they are drawn in.",
        )
    },
    authorName = "Ada Lovelace",
    authorAvatar = AVATAR_URL,
    authorUrlToken = "ada-lovelace",
    images = listOf(SampleImage),
    voteCount = 1284,
    commentCount = 96,
)

val sampleRelatedQueries = SearchItemDto.RelatedQueries(
    queries = listOf(
        "parser combinator",
        "parser versus lexer",
        "recursive descent parser",
        "segment tree",
    ),
)

val sampleRefinements = listOf(
    SuggestedQueryDto(suffix = "tutorial", query = "parser tutorial"),
    SuggestedQueryDto(suffix = "in Kotlin", query = "parser in Kotlin"),
    SuggestedQueryDto(suffix = "from scratch", query = "parser from scratch"),
)

val sampleHotQueries = listOf(
    HotQueryDto(query = "The parser that reads this feed", isHot = true),
    HotQueryDto(query = "Why a segment tree is not a tree", isHot = false),
)

// Reading

val sampleReadHistory = ReadHistoryDto(
    questionTitle = "How does a parser turn segments into elements?",
    questionToken = "19550972",
    contentTypeIcon = null,
    contentType = "answer",
    authorName = "Ada Lovelace",
    summary = "A segment arrives as a tree and leaves as a flat list of things a composable " +
        "can draw.",
    coverImage = PHOTO_URL,
    voteCount = 1284,
    commentCount = 96,
    readProgress = 42,
    answerCount = 0,
    followerCount = 0,
    contentCount = 0,
    playCount = 0,
    contentToken = "734558056",
    readTime = 1791726117L,
)

/** A question rather than an answer: no author, no cover, and the two counts that are its own. */
val sampleReadHistoryQuestion = ReadHistoryDto(
    questionTitle = "What is the difference between a segment and an element?",
    questionToken = "19550973",
    contentTypeIcon = null,
    contentType = "question",
    authorName = null,
    summary = null,
    coverImage = null,
    voteCount = 0,
    commentCount = 0,
    readProgress = 100,
    answerCount = 128,
    followerCount = 4200,
    contentCount = 0,
    playCount = 0,
    contentToken = null,
    readTime = 1791726117L,
)

/** A column: the one kind with no body under it, whose line carries its author and its counts. */
val sampleReadHistoryColumn = ReadHistoryDto(
    questionTitle = "Notes on parsing",
    questionToken = "",
    contentTypeIcon = null,
    contentType = "column",
    authorName = "Ada Lovelace",
    summary = null,
    coverImage = null,
    voteCount = 4299,
    commentCount = 0,
    readProgress = 0,
    answerCount = 0,
    followerCount = 0,
    contentCount = 808,
    playCount = 0,
    contentToken = "c_1867989249418211328",
    readTime = 1791726117L,
)

/** A video: a body under it like an answer's, and a line that counts plays rather than comments. */
val sampleReadHistoryVideo = ReadHistoryDto(
    questionTitle = "How a parser walks a segment tree",
    questionToken = "",
    contentTypeIcon = null,
    contentType = "zvideo",
    authorName = "Ada Lovelace",
    summary = "Six minutes on the shape the marks make, drawn out on a whiteboard.",
    coverImage = PHOTO_URL,
    voteCount = 230,
    commentCount = 0,
    readProgress = 0,
    answerCount = 0,
    followerCount = 0,
    contentCount = 0,
    playCount = 32000,
    contentToken = "1990470495565550724",
    readTime = 1791726117L,
)

val sampleCollectionItem = CollectionItemDto(
    id = "734558056",
    type = "answer",
    title = "How does a parser turn segments into elements?",
    authorName = "Ada Lovelace",
    authorAvatar = AVATAR_URL,
    excerpt = "The marks on the leaves are what decide which of them a paragraph becomes.",
    thumbnail = PHOTO_URL,
    voteCount = 1284,
    commentCount = 96,
    collectTime = 1791726117L,
    url = "https://www.zhihu.com/answer/734558056",
    collectionNames = listOf("Reading list", "Parsing"),
)

// Columns

val sampleColumn = ColumnDto(
    urlToken = "c_1867989249418211328",
    title = "Notes on parsing",
    intro = "A column about turning text into something a program can walk.",
    imageUrl = PHOTO_URL,
    author = samplePerson,
    followers = 12400,
    viewsCount = 98211,
    voteupCount = 3210,
    isFollowing = false,
    authorAgree = 12000,
    authorLike = 8400,
    authorFavorite = 2100,
    authorInfluenceWords = emptyList(),
)

/** The same column for an author whose counts could not be read, whose words are drawn instead. */
val sampleColumnInWords = sampleColumn.copy(
    authorAgree = null,
    authorLike = null,
    authorFavorite = null,
    authorInfluenceWords = listOf("Agreed with 12,000 times", "Liked 8,400 times"),
    isFollowing = true,
)

val sampleColumnItems = listOf(
    ColumnItemDto(
        id = "734558056",
        title = "How does a parser turn segments into elements?",
        excerpt = "The marks on the leaves are what decide which of them a paragraph becomes.",
        created = 1791726117L,
        imageUrl = PHOTO_URL,
        voteupCount = 1284,
        commentCount = 96,
        type = "answer",
    ),
    ColumnItemDto(
        id = "734558057",
        title = "A segment tree is not a tree",
        excerpt = "It is the shape the marks make, and it is flat.",
        created = 1791726000L,
        imageUrl = null,
        voteupCount = 12,
        commentCount = 0,
        type = "article",
    ),
)

// Comments

private val SampleCommentAuthor = CommentAuthor(
    id = "1a2b3c4d5e6f7081",
    name = "Grace Hopper",
    avatarUrl = AVATAR_URL,
    headline = "Compilers, and why the tape does not care how you indent",
    vipInfo = null,
    userType = "people",
)

private val SampleReplyAuthor = Author(
    id = "8f2a1b3c4d5e6f70",
    name = "Ada Lovelace",
    avatarUrl = AVATAR_URL,
    headline = "Writes about parsers and the machines that read them",
)

val sampleComment = CommentDto(
    id = "11594201738",
    author = SampleCommentAuthor,
    replyToAuthor = null,
    createdTime = 1791726117L,
    likeCount = 42,
    liked = false,
    isAuthor = false,
    isAuthorTop = false,
    childCount = 3,
    childComments = emptyList(),
    ipInfo = "Fujian",
    parsedContent = CommentContent(
        text = AnnotatedString(
            "The tree is only half the story: the marks on the leaves decide what is drawn, " +
                "and they are what the cache keys on.",
        ),
        images = emptyList(),
    ),
    canReply = true,
)

/** A reply: it names who it answers, it is liked, and it has no replies of its own. */
val sampleReply = sampleComment.copy(
    id = "11594201739",
    replyToAuthor = SampleReplyAuthor,
    likeCount = 0,
    liked = true,
    childCount = 0,
)

/** A comment the server will not take a reply on, which hides the way in. */
val sampleUnreplyableComment = sampleComment.copy(
    id = "11594201740",
    likeCount = 1,
    childCount = 0,
    canReply = false,
)

/** Left by the author of the piece it sits under, which is what the row's badge says. */
val sampleCommentByAuthor = sampleComment.copy(
    id = "11594201741",
    isAuthor = true,
)

/** The author answering someone: the badge on a row that also names who it answers. */
val sampleReplyByAuthor = sampleReply.copy(
    id = "11594201742",
    isAuthor = true,
)

val sampleCommentByTop = sampleComment.copy(
    id = "11594201743",
    isAuthorTop = true,
)

/** A comment with replies under it, one of which the row draws above the way into the rest. */
val sampleCommentWithReplies = sampleComment.copy(
    id = "11594201744",
    childCount = 5,
    childComments = listOf(sampleReply),
)

val sampleComments = listOf(sampleComment, sampleReply, sampleUnreplyableComment)

// Messages

val samplePeer = ChatPeerDto(
    name = "Ada Lovelace",
    avatarUrl = AVATAR_URL,
    headline = "Writes about parsers and the machines that read them",
    isForbidden = false,
)

val sampleMessages = listOf(
    ChatMessageDto(
        id = "1",
        text = AnnotatedString("Did the parser land?"),
        isMine = false,
        createdTime = 1791726117L,
    ),
    ChatMessageDto(
        id = "2",
        text = AnnotatedString("It did — segments in, elements out."),
        isMine = true,
        createdTime = 1791726137L,
    ),
    ChatMessageDto(
        id = "3",
        text = AnnotatedString(
            "Then the cache is the next thing: one parse serves both themes, so the key is " +
                "the body and nothing else.",
        ),
        isMine = false,
        createdTime = 1791726200L,
    ),
)

val sampleThread = MessageThreadDto(
    id = "1",
    peerId = "8f2a1b3c4d5e6f70",
    sourceType = null,
    title = "Ada Lovelace",
    text = "Then the cache is the next thing: one parse serves both themes.",
    avatarUrl = AVATAR_URL,
    unreadCount = 2,
    createdTime = 1791726200L,
    targetLink = "",
)

val sampleThreads = listOf(
    sampleThread,
    sampleThread.copy(
        id = "2",
        title = "Grace Hopper",
        text = "The tape does not care how you indent.",
        unreadCount = 0,
    ),
)

val sampleCategories = listOf(
    NotificationCategoryDto(
        type = "vote",
        title = "Agreements",
        text = "Who agreed with what you wrote",
        unreadCount = 12,
    ),
    NotificationCategoryDto(
        type = "comment",
        title = "Comments",
        text = "Answers and replies left for you",
        unreadCount = 3,
    ),
    NotificationCategoryDto(
        type = "follow",
        title = "Followers",
        text = "Who started following you",
        unreadCount = 0,
    ),
)

val sampleNotificationDay = NotificationItemDto.Day(text = "Today", count = 4)

val sampleNotificationEntry = NotificationItemDto.Entry(
    id = "1791726117",
    title = "Ada Lovelace",
    subTitle = "agreed with your answer",
    text = buildAnnotatedString { append("How does a parser turn segments into elements?") },
    targetText = "The marks on the leaves are what decide which of them a paragraph becomes.",
    isContentDeleted = false,
    label = "Agreed",
    avatarUrl = AVATAR_URL,
    unreadCount = 1,
    isRead = false,
    createdTime = 1791726117L,
    targetLink = "",
)

// Moments

val sampleMoment = MomentDto(
    id = "1791726117",
    type = MomentContentType.ANSWER,
    title = "How does a parser turn segments into elements?",
    plainContent = "A segment arrives as a tree and leaves as a flat list.",
    summary = "The marks on the leaves are what decide which of them a paragraph becomes.",
    authorName = "Ada Lovelace",
    authorAvatarUrl = AVATAR_URL,
    routerUrl = "https://www.zhihu.com/answer/734558056",
    voteCount = 1284,
    commentCount = 96,
    collectCount = 210,
    mediaImages = listOf(MediaImage(url = PHOTO_URL, width = 1200, height = 800)),
    videoThumbnail = null,
    publishedAt = 1791726117L,
    actionText = "Published an answer",
    actionTime = 1791726117L,
    isTopping = false,
)

/** A thought: no question over it, and what it says is the whole of it. */
val sampleThought = sampleMoment.copy(
    id = "1791726118",
    type = MomentContentType.THOUGHT,
    title = "",
    plainContent = "Reading a parse tree is easier than reading the parser that built it.",
    summary = "Reading a parse tree is easier than reading the parser that built it.",
    mediaImages = emptyList(),
    actionText = "Shared a thought",
    isTopping = true,
)

// Questions

val sampleTopic = Topic(id = "19550972", name = "Parsing", avatarUrl = AVATAR_URL)

val sampleTopics = listOf(
    sampleTopic,
    Topic(id = "19550973", name = "Kotlin", avatarUrl = AVATAR_URL),
    Topic(id = "19550974", name = "Compilers", avatarUrl = null),
)

val sampleQuestion = QuestionDetail(
    id = 19550972L,
    title = "How does a parser turn segments into elements?",
    detail = "I keep reading about segments and elements and cannot tell where one becomes the " +
        "other. Is the tree kept anywhere, or does it only exist while the body is parsed?",
    author = sampleUser,
    topics = sampleTopics,
    answerCount = 128,
    commentCount = 12,
    followerCount = 4200,
    updatedTime = 1791726117L,
)

// Feedback

val sampleFeedbackActions = listOf<FeedbackAction>(
    FeedbackAction.Request(
        label = "Show fewer answers like this",
        moduleId = "ignore_reduce_similar",
        iconUrl = null,
        nightIconUrl = null,
        hasChevron = false,
        maxLines = 1,
        url = "https://api.zhihu.com/feed/feedback",
        method = "POST",
        toastText = "Got it",
    ),
    FeedbackAction.Request(
        label = "Show fewer answers from this author",
        moduleId = "ignore_author",
        iconUrl = null,
        nightIconUrl = null,
        hasChevron = false,
        maxLines = 1,
        url = "https://api.zhihu.com/feed/feedback",
        method = "POST",
        toastText = null,
    ),
    FeedbackAction.OpenUrl(
        label = "Block topics and keywords",
        moduleId = "block_list",
        iconUrl = null,
        nightIconUrl = null,
        hasChevron = true,
        maxLines = 1,
        url = "https://api.zhihu.com/feed/block",
    ),
)

// Rich text

val sampleParagraph = RichTextElement.ParsedText(
    content = AnnotatedString(
        "A segment arrives as a tree and leaves as a flat list of things a composable can draw. " +
            "The marks on the text are what decide which of them a paragraph becomes, and the " +
            "order of the marks is the order they are drawn in.",
    ),
    inlineMetas = emptyList(),
)

val sampleHeading = RichTextElement.Heading(
    content = AnnotatedString("What a segment carries"),
    level = 2,
)

val sampleQuote = RichTextElement.Blockquote(
    content = AnnotatedString(
        "One parse serves both themes, which is what lets its result be cached.",
    ),
    inlineMetas = emptyList(),
)

val sampleBullets = listOf(
    RichTextElement.BulletItem(
        content = AnnotatedString("A paragraph is a leaf, and it is the common case"),
        inlineMetas = emptyList(),
        level = 0,
        isOrdered = false,
    ),
    RichTextElement.BulletItem(
        content = AnnotatedString("A list holds items, each of which is parsed on its own"),
        inlineMetas = emptyList(),
        level = 0,
        isOrdered = false,
    ),
    RichTextElement.BulletItem(
        content = AnnotatedString("and one level in, the same thing again"),
        inlineMetas = emptyList(),
        level = 1,
        isOrdered = false,
    ),
)

val sampleTable = RichTextElement.Table(
    rows = 3,
    cols = 3,
    cells = listOf(
        RichTextElement.TableCell(AnnotatedString("Kind"), emptyList()),
        RichTextElement.TableCell(AnnotatedString("Holds"), emptyList()),
        RichTextElement.TableCell(AnnotatedString("Drawn by"), emptyList()),
        RichTextElement.TableCell(AnnotatedString("Paragraph"), emptyList()),
        RichTextElement.TableCell(AnnotatedString("Runs of text"), emptyList()),
        RichTextElement.TableCell(AnnotatedString("ZRichText"), emptyList()),
        RichTextElement.TableCell(AnnotatedString("Code"), emptyList()),
        RichTextElement.TableCell(AnnotatedString("Ranges, no color"), emptyList()),
        RichTextElement.TableCell(AnnotatedString("CodeBlock"), emptyList()),
    ),
    hasHeader = true,
)

private const val SAMPLE_CODE = "// What a cart is worth, in cents.\n" +
    "fun price(items: List<Int>): Int {\n" +
    "    return if (items.size > 3) 12 else items.size\n" +
    "}"

val sampleCode = RichTextElement.Code(
    code = SAMPLE_CODE,
    lang = "kotlin",
    tokens = listOf(
        CodeToken(start = 0, end = 34, kind = CodeTokenKind.Comment),
        CodeToken(start = 35, end = 38, kind = CodeTokenKind.Keyword),
        CodeToken(start = 39, end = 44, kind = CodeTokenKind.Mark),
        CodeToken(start = 45, end = 51, kind = CodeTokenKind.Mark),
        CodeToken(start = 52, end = 58, kind = CodeTokenKind.Mark),
        CodeToken(start = 60, end = 63, kind = CodeTokenKind.Keyword),
    ),
)

/** A fence naming a language the tokenizer does not carry: one color, no runs. */
val samplePlainCode = RichTextElement.Code(
    code = "SELECT id, title\nFROM segments\nWHERE kind = 'paragraph';",
    lang = "sql",
    tokens = emptyList(),
)

/**
 * The addresses three captured formulas were rendered at. They are the API's own, and they are here
 * because a formula is a bitmap the server rasterized, not something drawn locally -- so with no
 * network a preview shows the placeholder instead, at the same dp box these numbers ask for, which
 * is the half worth looking at anyway.
 */
private const val WIDE_FORMULA_URL =
    "https://picx.zhimg.com/v2-bdfa9d725446079f8ab0e42ccb8d855f.jpg" +
        "?source=7e7ef6e2&needBackground=1"

private const val TALL_FORMULA_URL =
    "https://picx.zhimg.com/v2-70897a1a53e0fb13afcfc2d38fbb98f8.jpg" +
        "?source=7e7ef6e2&needBackground=1"

/** The id the paragraph below names its formula by, which is what pairs it with its [Formula]. */
private const val INLINE_FORMULA_ID = "1"

val sampleFormulaBlock = RichTextElement.FormulaBlock(
    data = Formula(content = "n \\times n = n^2", imgUrl = WIDE_FORMULA_URL, width = 84, height = 20),
)

/**
 * A formula tall enough to break the line it stands in: 89dp against a line height of about 20,
 * which is the case `withFormulaLineHeight` exists for.
 */
val sampleInlineFormula = Formula(
    content = "\\begin{aligned} C_{11} &= A_{11}B_{11} + A_{12}B_{21} \\\\ " +
        "C_{12} &= A_{11}B_{12} + A_{12}B_{22} \\end{aligned}",
    imgUrl = TALL_FORMULA_URL,
    width = 186,
    height = 89,
)

/** A paragraph whose one inline formula stands taller than its own line. */
val sampleFormulaParagraph = RichTextElement.ParsedText(
    content = buildAnnotatedString {
        append("A line with a formula in it ")
        appendInlineContent(INLINE_FORMULA_ID, sampleInlineFormula.content)
        append(", and the row has to be tall enough to hold the whole of it.")
    },
    inlineMetas = listOf(
        InlineFormulaMeta(formula = sampleInlineFormula, inlineId = INLINE_FORMULA_ID),
    ),
)

val sampleCard = RichTextElement.Card(
    cardType = "link-card",
    title = "A parse tree, drawn",
    url = "https://example.com/parse-tree",
    cover = PHOTO_URL,
    contentType = null,
    externalLink = "example.com",
    voteCount = 1284,
    commentCount = 96,
)

val sampleColumnCard = RichTextElement.ColumnCard(
    columnToken = "c_1867989249418211328",
    title = "Notes on parsing",
    desc = "A column about turning text into something a program can walk.",
    cover = PHOTO_URL,
    headerTips = "From the column · updated this week",
    latestText = "A segment tree is not a tree",
)

val sampleReference = RichTextElement.Reference(
    items = listOf(
        RichTextElement.ParsedText(
            content = AnnotatedString("The first footnote, and it says something."),
            inlineMetas = emptyList(),
        ),
        RichTextElement.ParsedText(
            content = AnnotatedString("The second, which is shorter."),
            inlineMetas = emptyList(),
        ),
    ),
)

val sampleRichTextElements = listOf(
    sampleHeading,
    sampleParagraph,
    sampleQuote,
    sampleCode,
    samplePlainCode,
    sampleTable,
    sampleCard,
    sampleColumnCard,
    sampleFormulaBlock,
    RichTextElement.Divider,
    sampleReference,
)

val sampleImages = listOf(SampleImage, SampleWideImage)

val sampleZhihuImage = SampleZhihuImage

// The body's author row

/**
 * The author whose row opens a body: a name, a line about them, and the button beside it -- left
 * not following, which is the state a reader meets it in.
 */
val sampleAnswerAuthor = AnswerAuthor(
    fullname = "Ada Lovelace",
    urlToken = "ada",
    description = "Writes about parsers and the machines that read them",
)
