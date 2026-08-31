package com.purehub.app.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

private val vi = mapOf(
    "Home" to "Trang chủ", "Tools" to "Công cụ", "Community" to "Cộng đồng", "Settings" to "Cài đặt", "Help" to "Trợ giúp",
    "Continue" to "Tiếp tục", "Cancel" to "Hủy", "Close" to "Đóng", "Save" to "Lưu", "Delete" to "Xóa", "Remove" to "Xóa",
    "Clear" to "Xóa", "Reset" to "Đặt lại", "Start" to "Bắt đầu", "Stop" to "Dừng", "Pause" to "Tạm dừng", "Resume" to "Tiếp tục",
    "Share" to "Chia sẻ", "Export" to "Xuất", "Import" to "Nhập", "Copy" to "Sao chép", "Choose" to "Chọn", "Search" to "Tìm kiếm",
    "Open" to "Mở", "Lock" to "Khóa", "Unlock" to "Mở khóa", "Add" to "Thêm", "Edit" to "Chỉnh sửa", "Apply" to "Áp dụng",
    "Title" to "Tiêu đề", "Category" to "Danh mục", "Note" to "Ghi chú", "Amount" to "Số tiền", "People" to "Số người",
    "Options" to "Tùy chọn", "Group" to "Nhóm", "Language" to "Ngôn ngữ", "Today" to "Hôm nay", "History" to "Lịch sử",
    "All" to "Tất cả", "Favorites" to "Yêu thích", "Quick access" to "Truy cập nhanh", "No results" to "Không có kết quả",
    "Scan receipt" to "Quét hóa đơn", "Save expense" to "Lưu chi tiêu", "Export CSV" to "Xuất CSV", "No expenses logged yet." to "Chưa có khoản chi nào.",
    "Expense" to "Chi tiêu", "Income" to "Thu nhập", "Expenses" to "Chi tiêu", "Monthly balance" to "Số dư tháng", "Save income" to "Lưu thu nhập",
    "Wallet or account" to "Ví hoặc tài khoản", "Search title, category or wallet" to "Tìm tiêu đề, danh mục hoặc ví", "Include microphone" to "Thu âm microphone", "Clean all pages" to "Làm sạch tất cả trang",
    "No matching expense." to "Không có khoản chi phù hợp.", "Monthly budget" to "Ngân sách tháng", "Search title or category" to "Tìm tiêu đề hoặc danh mục",
    "Document title" to "Tên tài liệu", "Capture Page" to "Chụp trang", "Export PDF" to "Xuất PDF", "Open PDF" to "Mở PDF", "Share PDF" to "Chia sẻ PDF",
    "Apply Crop" to "Áp dụng cắt", "Move Left" to "Sang trái", "Move Right" to "Sang phải", "Allow Camera for Doc Capture" to "Cho phép camera để chụp tài liệu",
    "Create ZIP" to "Tạo ZIP", "Calculate SHA-256" to "Tính SHA-256", "Copy HEX" to "Sao chép HEX", "Spin" to "Quay",
    "Monitoring" to "Đang đo", "Reset Peak" to "Đặt lại đỉnh", "Recent sessions" to "Phiên gần đây", "Enable compass" to "Bật la bàn", "Pause compass" to "Tạm dừng la bàn",
    "Calibrate zero" to "Hiệu chỉnh về 0", "Hold reading" to "Giữ kết quả", "Enable level sensor" to "Bật cảm biến cân bằng", "Pause level sensor" to "Tạm dừng cảm biến",
    "Scan" to "Quét", "Scanning" to "Đang quét", "Keep files" to "Giữ tệp", "Storage review" to "Kiểm tra bộ nhớ", "Unnamed file" to "Tệp chưa đặt tên",
    "Encrypted private backup" to "Sao lưu riêng tư được mã hóa", "Backup passphrase (8+ characters)" to "Mật khẩu sao lưu (từ 8 ký tự)",
    "Search accounts" to "Tìm tài khoản", "Account label" to "Tên tài khoản", "Base32 secret or otpauth:// URI" to "Mã bí mật Base32 hoặc URI otpauth://",
    "Encrypt & add" to "Mã hóa và thêm", "Unlock with device security" to "Mở khóa bằng bảo mật thiết bị", "No account or network connection is used." to "Không dùng tài khoản hoặc kết nối mạng.",
    "Start cleaning cycle" to "Bắt đầu chu kỳ làm sạch", "Cycle complete" to "Chu kỳ hoàn tất", "Free for everyone" to "Miễn phí cho mọi người",
    "Open Telegram" to "Mở Telegram", "Open GitHub" to "Mở GitHub", "Send feedback or report a bug" to "Gửi góp ý hoặc báo lỗi",
    "All tools" to "Tất cả công cụ", "Free, private and ad-free utilities. Search by what you need to do." to "Công cụ miễn phí, riêng tư và không quảng cáo. Tìm theo nhu cầu của bạn.",
    "Privacy & preferences" to "Quyền riêng tư và tùy chọn", "Offline-first" to "Ưu tiên ngoại tuyến", "Private" to "Riêng tư",
    "Area" to "Diện tích", "Volume" to "Thể tích", "Speed" to "Tốc độ", "Time" to "Thời gian", "Pressure" to "Áp suất",
    "Energy" to "Năng lượng", "Power" to "Công suất", "Angle" to "Góc", "Search units" to "Tìm đơn vị", "Swap" to "Đảo chiều",
    "Favorite" to "Yêu thích", "Unfavorite" to "Bỏ yêu thích", "Save to history" to "Lưu lịch sử", "Copy result" to "Sao chép kết quả",
    "Favorite conversions" to "Phép đổi yêu thích", "Recent conversions" to "Phép đổi gần đây", "Import images" to "Nhập hình ảnh",
    "Metadata found" to "Metadata tìm thấy", "From" to "Từ", "To" to "Sang", "Value" to "Giá trị",
    "Level sound on" to "Bật âm báo cân bằng", "Level sound off" to "Tắt âm báo cân bằng",
)

private val zh = mapOf(
    "Home" to "主页", "Tools" to "工具", "Community" to "社区", "Settings" to "设置", "Help" to "帮助",
    "Continue" to "继续", "Cancel" to "取消", "Close" to "关闭", "Save" to "保存", "Delete" to "删除", "Remove" to "移除",
    "Clear" to "清除", "Reset" to "重置", "Start" to "开始", "Stop" to "停止", "Pause" to "暂停", "Resume" to "继续",
    "Share" to "分享", "Export" to "导出", "Import" to "导入", "Copy" to "复制", "Choose" to "选择", "Search" to "搜索",
    "Open" to "打开", "Lock" to "锁定", "Unlock" to "解锁", "Add" to "添加", "Edit" to "编辑", "Apply" to "应用",
    "Title" to "标题", "Category" to "类别", "Note" to "备注", "Amount" to "金额", "People" to "人数",
    "Options" to "选项", "Group" to "分组", "Language" to "语言", "Today" to "今天", "History" to "历史",
    "All" to "全部", "Favorites" to "收藏", "Quick access" to "快速访问", "No results" to "无结果",
    "Scan receipt" to "扫描收据", "Save expense" to "保存支出", "Export CSV" to "导出 CSV", "No expenses logged yet." to "尚无支出记录。",
    "Expense" to "支出", "Income" to "收入", "Expenses" to "支出", "Monthly balance" to "月度结余", "Save income" to "保存收入",
    "Wallet or account" to "钱包或账户", "Search title, category or wallet" to "搜索标题、类别或钱包", "Include microphone" to "包含麦克风", "Clean all pages" to "清理所有页面",
    "No matching expense." to "没有匹配的支出。", "Monthly budget" to "月度预算", "Search title or category" to "搜索标题或类别",
    "Document title" to "文档标题", "Capture Page" to "拍摄页面", "Export PDF" to "导出 PDF", "Open PDF" to "打开 PDF", "Share PDF" to "分享 PDF",
    "Apply Crop" to "应用裁剪", "Move Left" to "左移", "Move Right" to "右移", "Allow Camera for Doc Capture" to "允许相机拍摄文档",
    "Create ZIP" to "创建 ZIP", "Calculate SHA-256" to "计算 SHA-256", "Copy HEX" to "复制 HEX", "Spin" to "旋转",
    "Monitoring" to "监测中", "Reset Peak" to "重置峰值", "Recent sessions" to "最近会话", "Enable compass" to "启用指南针", "Pause compass" to "暂停指南针",
    "Calibrate zero" to "校准零点", "Hold reading" to "保持读数", "Enable level sensor" to "启用水平传感器", "Pause level sensor" to "暂停水平传感器",
    "Scan" to "扫描", "Scanning" to "扫描中", "Keep files" to "保留文件", "Storage review" to "存储检查", "Unnamed file" to "未命名文件",
    "Encrypted private backup" to "加密私密备份", "Backup passphrase (8+ characters)" to "备份密码（至少 8 个字符）",
    "Search accounts" to "搜索账户", "Account label" to "账户名称", "Base32 secret or otpauth:// URI" to "Base32 密钥或 otpauth:// URI",
    "Encrypt & add" to "加密并添加", "Unlock with device security" to "使用设备安全解锁", "No account or network connection is used." to "无需账户或网络连接。",
    "Start cleaning cycle" to "开始清洁周期", "Cycle complete" to "周期完成", "Free for everyone" to "永久免费",
    "Open Telegram" to "打开 Telegram", "Open GitHub" to "打开 GitHub", "Send feedback or report a bug" to "发送反馈或报告错误",
    "All tools" to "全部工具", "Free, private and ad-free utilities. Search by what you need to do." to "免费、私密且无广告的工具。按需求搜索。",
    "Privacy & preferences" to "隐私与偏好", "Offline-first" to "离线优先", "Private" to "隐私",
    "Area" to "面积", "Volume" to "体积", "Speed" to "速度", "Time" to "时间", "Pressure" to "压力",
    "Energy" to "能量", "Power" to "功率", "Angle" to "角度", "Search units" to "搜索单位", "Swap" to "互换",
    "Favorite" to "收藏", "Unfavorite" to "取消收藏", "Save to history" to "保存到历史", "Copy result" to "复制结果",
    "Favorite conversions" to "收藏的换算", "Recent conversions" to "最近换算", "Import images" to "导入图片",
    "Metadata found" to "发现元数据", "From" to "从", "To" to "到", "Value" to "数值",
    "Level sound on" to "开启水平提示音", "Level sound off" to "关闭水平提示音",
)

private val viMore = mapOf(
    "Overview" to "Tổng quan", "Large files" to "Tệp lớn", "Duplicates" to "Tệp trùng", "Potential space · nothing auto-selected" to "Dung lượng có thể giải phóng · không tự động chọn tệp",
    "Offline SHA-256 matching · Android confirms deletion" to "Đối chiếu SHA-256 ngoại tuyến · Android xác nhận trước khi xóa", "KEEP" to "GIỮ LẠI",
    "Unnamed image" to "Ảnh chưa đặt tên", "Media" to "Đa phương tiện", "Images" to "Hình ảnh", "Videos" to "Video", "Audio" to "Âm thanh",
    "Vietnamese lunar dates, can-chi and traditional markers." to "Ngày âm Việt Nam, can chi và các dấu mốc truyền thống.",
    "Build simple habits without accounts or pressure." to "Xây dựng thói quen đơn giản, không tài khoản hay áp lực.",
    "A calm focus timer with local soundscapes." to "Đồng hồ tập trung nhẹ nhàng với âm thanh lưu trên máy.",
    "Guided breathing with gentle motion." to "Hướng dẫn hít thở với chuyển động nhẹ nhàng.", "Direction, bearing and sensor guidance." to "Phương hướng, góc phương vị và hướng dẫn cảm biến.",
    "Quick two-axis leveling and calibration." to "Cân bằng hai trục và hiệu chỉnh nhanh.", "Private estimated sound-level monitoring." to "Ước tính độ ồn riêng tư.",
    "Torch, screen light and safety patterns." to "Đèn pin, đèn màn hình và tín hiệu an toàn.", "Fast offline unit conversion." to "Đổi đơn vị ngoại tuyến nhanh chóng.",
    "Scan QR codes and barcodes, create codes, and keep a private local history." to "Quét QR và mã vạch, tạo mã và lưu lịch sử riêng tư trên máy.",
    "Capture, arrange and export private PDFs." to "Chụp, sắp xếp và xuất PDF riêng tư.", "Scan, clean, edit and export private documents offline." to "Quét, làm sạch, sửa và xuất tài liệu riêng tư ngoại tuyến.",
    "Sample and copy colors from the camera." to "Lấy mẫu và sao chép màu từ camera.", "Create share-ready photos without GPS or EXIF metadata." to "Tạo ảnh sẵn sàng chia sẻ, không còn GPS hay metadata EXIF.",
    "Review reclaimable files before deleting." to "Xem lại các tệp có thể giải phóng trước khi xóa.", "Play a controlled tone for residual water." to "Phát âm thanh kiểm soát để đẩy nước còn sót.",
    "Inspect nearby signals and Wi-Fi channels." to "Kiểm tra tín hiệu lân cận và các kênh Wi-Fi.", "Encrypted local credentials with device protection." to "Thông tin đăng nhập mã hóa trên máy, được thiết bị bảo vệ.",
    "Offline 2FA codes protected by your device lock." to "Mã 2FA ngoại tuyến được khóa bảo vệ bởi thiết bị.", "Hash, archive and share local files privately." to "Băm, nén và chia sẻ tệp trên máy một cách riêng tư.",
    "Local wallpaper preview and rotation." to "Xem trước và luân phiên hình nền trên máy.", "Split items, tax and tips for a group." to "Chia món, thuế và tiền tip cho nhóm.",
    "Private offline expense ledger." to "Sổ chi tiêu ngoại tuyến riêng tư.", "A fair local picker for quick choices." to "Bộ chọn ngẫu nhiên công bằng cho quyết định nhanh.",
    "Telegram, GitHub and the PureHub roadmap." to "Telegram, GitHub và lộ trình PureHub.", "Record a local MP4 with Android's consent flow." to "Quay MP4 trên máy qua bước xác nhận của Android.",
    "TODAY" to "HÔM NAY", "Lunar" to "Âm lịch", "Mon" to "T2", "Tue" to "T3", "Wed" to "T4", "Thu" to "T5", "Fri" to "T6", "Sat" to "T7", "Sun" to "CN",
    "Calendar Suite flagship" to "Bộ lịch nổi bật", "Browse solar and lunar dates instantly with private, on-device conversion that works without a signal." to "Xem ngày dương và âm lịch tức thì; chuyển đổi riêng tư trên thiết bị, không cần mạng.",
    "Tet Nguyen Dan" to "Tết Nguyên Đán", "Ram thang Gieng" to "Rằm tháng Giêng", "Tet Han Thuc" to "Tết Hàn Thực", "Gio To Hung Vuong" to "Giỗ Tổ Hùng Vương",
    "Phat Dan" to "Phật Đản", "Tet Doan Ngo" to "Tết Đoan Ngọ", "Vu Lan" to "Vu Lan", "Tet Trung Thu" to "Tết Trung Thu", "Ong Cong Ong Tao" to "Ông Công Ông Táo",
    "Zen & Time" to "Thiền & Thời gian", "Measure & Tools" to "Đo lường & Công cụ", "Vision" to "Hình ảnh",
    "System & Security" to "Hệ thống & Bảo mật", "Finance & Community" to "Tài chính & Cộng đồng",
    "Lunar Calendar" to "Lịch âm", "Zen Habit" to "Thói quen tích cực", "Zen Pomodoro" to "Pomodoro tập trung", "Zen Breath" to "Hít thở thư giãn",
    "Compass" to "La bàn", "Bubble Level & Ruler" to "Thước cân bằng & Thước đo", "Decibel Meter" to "Máy đo độ ồn",
    "Smart Flashlight" to "Đèn pin thông minh", "Unit Converter" to "Đổi đơn vị", "QR Studio" to "Công cụ QR",
    "Doc to PDF" to "Tài liệu sang PDF", "OCR Studio" to "Quét chữ OCR", "Color Grabber" to "Lấy mã màu", "Photo Privacy" to "Bảo mật ảnh",
    "Deep Cleaner" to "Dọn dẹp sâu", "Speaker Cleaner" to "Làm sạch loa", "WiFi Analyzer" to "Phân tích Wi-Fi",
    "Password Vault" to "Kho mật khẩu", "Authenticator Vault" to "Kho mã xác thực", "File Studio" to "Công cụ tệp",
    "Wallpaper Changer" to "Đổi hình nền", "Bill Splitter" to "Chia hóa đơn", "Expense Tracker" to "Theo dõi chi tiêu",
    "Decision Wheel" to "Vòng quay quyết định", "PureHub Community" to "Cộng đồng PureHub", "Screen Recorder" to "Quay màn hình",
    "Manage Tools" to "Quản lý công cụ", "What do you want to do?" to "Bạn muốn làm gì?", "No tools enabled" to "Chưa bật công cụ nào",
    "No visible mini-apps." to "Không có tiện ích nào đang hiển thị.", "Search mini-app" to "Tìm tiện ích", "Apply Now" to "Áp dụng ngay",
    "FREE · NO ADS · OPEN SOURCE" to "MIỄN PHÍ · KHÔNG QUẢNG CÁO · MÃ NGUỒN MỞ",
    "Useful, private tools for everyday life—built openly with the community." to "Bộ công cụ hữu ích, riêng tư cho hằng ngày—được xây dựng công khai cùng cộng đồng.",
    "Review permissions, visible tools and local-only storage behavior." to "Xem lại quyền truy cập, công cụ hiển thị và dữ liệu chỉ lưu trên máy.",
    "Permission Center" to "Trung tâm quyền", "Open Android app settings" to "Mở cài đặt ứng dụng Android",
    "Permissions are requested only from the tool that needs them. PureHub has no INTERNET permission." to "Chỉ công cụ cần thiết mới yêu cầu quyền. PureHub không có quyền INTERNET.",
    "Allow camera" to "Cho phép camera", "Choose photo" to "Chọn ảnh", "Choose Local Images" to "Chọn ảnh trên máy", "Scan image" to "Quét ảnh",
    "Capture a page or choose an image to begin." to "Chụp một trang hoặc chọn ảnh để bắt đầu.", "No text yet" to "Chưa có nội dung",
    "Recognized text" to "Văn bản nhận dạng", "Recognition language" to "Ngôn ngữ nhận dạng", "Document cleanup" to "Làm sạch tài liệu",
    "Start scanning" to "Bắt đầu quét", "Add page" to "Thêm trang", "New document" to "Tài liệu mới", "Private library" to "Thư viện riêng tư",
    "Search scans" to "Tìm bản quét", "Searchable and stored only on this device." to "Có thể tìm kiếm và chỉ lưu trên thiết bị này.",
    "Scan, clean and export text without uploading your documents." to "Quét, làm sạch và xuất chữ mà không tải tài liệu lên mạng.",
    "Create a code" to "Tạo mã", "Pick a format and fill in only the information people need." to "Chọn định dạng và chỉ điền thông tin cần thiết.",
    "Scan another" to "Quét mã khác", "Search history" to "Tìm lịch sử", "No saved codes yet" to "Chưa có mã đã lưu",
    "No matching codes." to "Không có mã phù hợp.", "Your scans and saved creations appear here." to "Mã đã quét và tạo sẽ xuất hiện tại đây.",
    "Camera stays off until you allow it" to "Camera luôn tắt cho đến khi bạn cấp quyền", "OCR runs locally after each capture." to "OCR xử lý ngay trên máy sau mỗi lần chụp.",
    "Entry title" to "Tên mục", "Username" to "Tên đăng nhập", "Password" to "Mật khẩu", "Generate strong" to "Tạo mật khẩu mạnh",
    "Reveal" to "Hiện", "Hide" to "Ẩn", "Search entries" to "Tìm mục", "Copy Password" to "Sao chép mật khẩu", "Save Encrypted Entry" to "Lưu mục đã mã hóa",
    "Export private backup" to "Xuất bản sao lưu riêng tư", "Saved only on this phone." to "Chỉ lưu trên điện thoại này.",
    "Open Wi-Fi settings" to "Mở cài đặt Wi-Fi", "Nearby networks" to "Mạng lân cận", "Enable Nearby Scan" to "Bật quét mạng lân cận",
    "Channel pressure" to "Mức độ đông kênh", "Signal history" to "Lịch sử tín hiệu", "Frequency" to "Tần số",
    "Start session" to "Bắt đầu phiên", "Stop & save" to "Dừng & lưu", "Session goal" to "Mục tiêu phiên", "Gentle haptics" to "Rung nhẹ",
    "Reduce motion" to "Giảm chuyển động", "Soundscape" to "Âm thanh nền", "Small actions count" to "Mỗi việc nhỏ đều đáng ghi nhận",
    "Start with one gentle habit" to "Bắt đầu bằng một thói quen nhẹ nhàng", "Add habit" to "Thêm thói quen", "Create a habit" to "Tạo thói quen",
    "Habit name" to "Tên thói quen", "Why it matters (optional)" to "Lý do quan trọng (không bắt buộc)", "Weekly target" to "Mục tiêu tuần",
    "Pick something small enough to repeat." to "Chọn việc đủ nhỏ để có thể lặp lại.", "Read for 10 minutes" to "Đọc sách 10 phút",
    "Build consistency without accounts, feeds, ads or guilt." to "Xây dựng sự đều đặn, không tài khoản, bảng tin, quảng cáo hay áp lực.",
    "Breathe comfortably and stop if you feel dizzy or unwell." to "Hít thở thoải mái và dừng nếu bạn chóng mặt hoặc khó chịu.",
    "Start cleaning cycle" to "Bắt đầu làm sạch", "Play Morse" to "Phát mã Morse", "Stop Pattern" to "Dừng tín hiệu", "Custom Morse text" to "Nội dung Morse tùy chỉnh",
    "A-Z and spaces only." to "Chỉ dùng A-Z và khoảng trắng.", "Quality preset" to "Chất lượng", "Start recording" to "Bắt đầu quay", "Preparing…" to "Đang chuẩn bị…",
    "Photo selected" to "Đã chọn ảnh", "No photo selected" to "Chưa chọn ảnh", "Share PNG" to "Chia sẻ PNG", "Color" to "Màu sắc",
    "From" to "Từ", "To" to "Sang", "Value" to "Giá trị", "Per person" to "Mỗi người", "Add Item" to "Thêm món",
    "Remove Last" to "Xóa món cuối", "Save Preset" to "Lưu mẫu", "Rename Preset" to "Đổi tên mẫu", "Delete Preset" to "Xóa mẫu",
    "Preset name" to "Tên mẫu", "Item name" to "Tên món", "Share Summary" to "Chia sẻ tổng kết",
    "Create a code" to "Tạo mã", "Website" to "Trang web", "Phone" to "Điện thoại", "Location" to "Vị trí", "Calendar" to "Lịch", "Contact" to "Liên hệ",
    "Text" to "Văn bản", "Document" to "Tài liệu", "Receipt" to "Hóa đơn", "Original" to "Gốc", "Clean" to "Làm rõ", "Library" to "Thư viện",
    "Quick actions" to "Thao tác nhanh", "Open link" to "Mở liên kết", "Call" to "Gọi điện", "Open-source on GitHub" to "Mã nguồn mở trên GitHub",
    "Telegram community" to "Cộng đồng Telegram", "Get updates, discuss useful tools and help other PureHub users." to "Nhận cập nhật, trao đổi công cụ hữu ích và hỗ trợ người dùng PureHub khác.",
    "Read the code, report bugs, suggest a mini app, improve translations or submit a pull request." to "Xem mã nguồn, báo lỗi, đề xuất tiện ích, cải thiện bản dịch hoặc gửi pull request.",
    "For a bug, include the tool name, Android version and repeatable steps—never private files, passwords or API keys." to "Khi báo lỗi, hãy ghi tên công cụ, phiên bản Android và các bước tái hiện—không gửi tệp riêng tư, mật khẩu hoặc khóa API.",
)

private val zhMore = mapOf(
    "Overview" to "概览", "Large files" to "大文件", "Duplicates" to "重复文件", "Potential space · nothing auto-selected" to "可释放空间 · 不会自动选择文件",
    "Offline SHA-256 matching · Android confirms deletion" to "离线 SHA-256 匹配 · 删除前由 Android 确认", "KEEP" to "保留", "Unnamed image" to "未命名图片",
    "Media" to "媒体", "Images" to "图片", "Videos" to "视频", "Audio" to "音频",
    "Vietnamese lunar dates, can-chi and traditional markers." to "越南农历日期、干支和传统节日。", "Build simple habits without accounts or pressure." to "无需账户和压力，培养简单习惯。",
    "A calm focus timer with local soundscapes." to "带本地环境音的平静专注计时器。", "Guided breathing with gentle motion." to "以轻柔动画引导呼吸。",
    "Direction, bearing and sensor guidance." to "方向、方位角与传感器指引。", "Quick two-axis leveling and calibration." to "快速双轴水平测量与校准。",
    "Private estimated sound-level monitoring." to "私密的估算声级监测。", "Torch, screen light and safety patterns." to "手电筒、屏幕灯和安全闪烁模式。",
    "Fast offline unit conversion." to "快速离线单位换算。", "Scan QR codes and barcodes, create codes, and keep a private local history." to "扫描二维码和条形码、创建代码并保留本地私密历史。",
    "Capture, arrange and export private PDFs." to "拍摄、整理并导出私密 PDF。", "Scan, clean, edit and export private documents offline." to "离线扫描、清理、编辑和导出私密文档。",
    "Sample and copy colors from the camera." to "从相机取样并复制颜色。", "Create share-ready photos without GPS or EXIF metadata." to "创建不含 GPS 或 EXIF 信息的可分享照片。",
    "Review reclaimable files before deleting." to "删除前检查可清理文件。", "Play a controlled tone for residual water." to "播放受控音调以排出残留水分。",
    "Inspect nearby signals and Wi-Fi channels." to "检查附近信号和 Wi-Fi 信道。", "Encrypted local credentials with device protection." to "受设备保护的本地加密凭据。",
    "Offline 2FA codes protected by your device lock." to "由设备锁保护的离线 2FA 代码。", "Hash, archive and share local files privately." to "私密地校验、压缩和分享本地文件。",
    "Local wallpaper preview and rotation." to "本地壁纸预览与轮换。", "Split items, tax and tips for a group." to "为多人分摊项目、税费和小费。",
    "Private offline expense ledger." to "私密离线支出账本。", "A fair local picker for quick choices." to "用于快速选择的公平本地随机工具。",
    "Telegram, GitHub and the PureHub roadmap." to "Telegram、GitHub 与 PureHub 路线图。", "Record a local MP4 with Android's consent flow." to "通过 Android 授权流程录制本地 MP4。",
    "TODAY" to "今天", "Lunar" to "农历", "Mon" to "周一", "Tue" to "周二", "Wed" to "周三", "Thu" to "周四", "Fri" to "周五", "Sat" to "周六", "Sun" to "周日",
    "Calendar Suite flagship" to "旗舰日历套件", "Browse solar and lunar dates instantly with private, on-device conversion that works without a signal." to "即时查看公历和农历日期；完全在设备上私密换算，无需网络。",
    "Tet Nguyen Dan" to "越南春节", "Ram thang Gieng" to "正月十五", "Tet Han Thuc" to "寒食节", "Gio To Hung Vuong" to "雄王祭祖日", "Phat Dan" to "佛诞节",
    "Tet Doan Ngo" to "端午节", "Vu Lan" to "盂兰盆节", "Tet Trung Thu" to "中秋节", "Ong Cong Ong Tao" to "灶君节",
    "Zen & Time" to "禅意与时间", "Measure & Tools" to "测量与工具", "Vision" to "图像", "System & Security" to "系统与安全", "Finance & Community" to "财务与社区",
    "Lunar Calendar" to "农历", "Zen Habit" to "禅意习惯", "Zen Pomodoro" to "专注番茄钟", "Zen Breath" to "呼吸放松", "Compass" to "指南针",
    "Bubble Level & Ruler" to "水平仪与尺子", "Decibel Meter" to "分贝仪", "Smart Flashlight" to "智能手电筒", "Unit Converter" to "单位换算",
    "QR Studio" to "二维码工具", "Doc to PDF" to "文档转 PDF", "OCR Studio" to "OCR 文字识别", "Color Grabber" to "颜色提取", "Photo Privacy" to "照片隐私",
    "Deep Cleaner" to "深度清理", "Speaker Cleaner" to "扬声器清洁", "WiFi Analyzer" to "Wi-Fi 分析", "Password Vault" to "密码库",
    "Authenticator Vault" to "验证器保险库", "File Studio" to "文件工具", "Wallpaper Changer" to "壁纸更换", "Bill Splitter" to "账单分摊",
    "Expense Tracker" to "支出记录", "Decision Wheel" to "决策转盘", "PureHub Community" to "PureHub 社区", "Screen Recorder" to "屏幕录制",
    "Manage Tools" to "管理工具", "What do you want to do?" to "你想做什么？", "No tools enabled" to "尚未启用工具", "No visible mini-apps." to "没有可见的小工具。",
    "Search mini-app" to "搜索小工具", "Apply Now" to "立即应用", "FREE · NO ADS · OPEN SOURCE" to "免费 · 无广告 · 开源",
    "Useful, private tools for everyday life—built openly with the community." to "实用、私密的日常工具，与社区共同开放构建。",
    "Review permissions, visible tools and local-only storage behavior." to "查看权限、可见工具和仅本机存储设置。", "Permission Center" to "权限中心",
    "Open Android app settings" to "打开 Android 应用设置", "Allow camera" to "允许相机", "Choose photo" to "选择照片", "Choose Local Images" to "选择本机图片",
    "Scan image" to "扫描图片", "Capture a page or choose an image to begin." to "拍摄页面或选择图片以开始。", "No text yet" to "暂无文字",
    "Recognized text" to "识别文字", "Recognition language" to "识别语言", "Document cleanup" to "文档清理", "Start scanning" to "开始扫描",
    "Add page" to "添加页面", "New document" to "新建文档", "Private library" to "私密资料库", "Search scans" to "搜索扫描件",
    "Searchable and stored only on this device." to "可搜索且仅存储在此设备。", "Scan, clean and export text without uploading your documents." to "无需上传文档即可扫描、清理和导出文字。",
    "Create a code" to "创建二维码", "Pick a format and fill in only the information people need." to "选择格式并只填写必要信息。", "Scan another" to "继续扫描",
    "Search history" to "搜索历史", "No saved codes yet" to "暂无已保存代码", "No matching codes." to "没有匹配的代码。", "Your scans and saved creations appear here." to "扫描和创建的代码会显示在这里。",
    "Camera stays off until you allow it" to "获得允许前相机保持关闭", "OCR runs locally after each capture." to "每次拍摄后 OCR 均在本机运行。",
    "Entry title" to "条目标题", "Username" to "用户名", "Password" to "密码", "Generate strong" to "生成强密码", "Reveal" to "显示", "Hide" to "隐藏",
    "Search entries" to "搜索条目", "Copy Password" to "复制密码", "Save Encrypted Entry" to "保存加密条目", "Export private backup" to "导出私密备份",
    "Saved only on this phone." to "仅保存在此手机。", "Open Wi-Fi settings" to "打开 Wi-Fi 设置", "Nearby networks" to "附近网络", "Enable Nearby Scan" to "启用附近扫描",
    "Channel pressure" to "信道拥挤度", "Signal history" to "信号历史", "Start session" to "开始会话", "Stop & save" to "停止并保存", "Session goal" to "会话目标",
    "Gentle haptics" to "轻柔振动", "Reduce motion" to "减少动画", "Soundscape" to "环境音", "Start with one gentle habit" to "从一个轻松习惯开始",
    "Add habit" to "添加习惯", "Create a habit" to "创建习惯", "Habit name" to "习惯名称", "Why it matters (optional)" to "重要原因（可选）", "Weekly target" to "每周目标",
    "Pick something small enough to repeat." to "选择一件容易坚持的小事。", "Read for 10 minutes" to "阅读 10 分钟", "Start cleaning cycle" to "开始清洁",
    "Play Morse" to "播放摩尔斯码", "Stop Pattern" to "停止信号", "Custom Morse text" to "自定义摩尔斯文本", "A-Z and spaces only." to "仅限 A-Z 和空格。",
    "Quality preset" to "质量预设", "Start recording" to "开始录制", "Preparing…" to "准备中…", "Share PNG" to "分享 PNG", "Color" to "颜色",
    "From" to "从", "To" to "到", "Value" to "数值", "Per person" to "每人", "Add Item" to "添加项目", "Remove Last" to "删除最后一项",
    "Save Preset" to "保存预设", "Rename Preset" to "重命名预设", "Delete Preset" to "删除预设", "Preset name" to "预设名称", "Item name" to "项目名称",
    "Share Summary" to "分享汇总", "Website" to "网站", "Phone" to "电话", "Location" to "位置", "Calendar" to "日历", "Contact" to "联系人",
    "Text" to "文本", "Document" to "文档", "Receipt" to "收据", "Original" to "原图", "Clean" to "增强", "Library" to "资料库",
    "Quick actions" to "快捷操作", "Open link" to "打开链接", "Call" to "拨号", "Open-source on GitHub" to "GitHub 开源项目", "Telegram community" to "Telegram 社区",
)

private val viAudit = mapOf(
    "Community flagship" to "Cộng đồng nổi bật", "PureHub belongs to everyone" to "PureHub thuộc về mọi người",
    "Join the conversation, report issues and shape free, no-ad, open-source tools together." to "Cùng thảo luận, báo lỗi và xây dựng bộ công cụ miễn phí, không quảng cáo, mã nguồn mở.",
    "Zen Suite flagship" to "Bộ thư giãn nổi bật", "Sensor Suite" to "Bộ cảm biến", "Document Suite" to "Bộ tài liệu", "Security Suite flagship" to "Bộ bảo mật nổi bật",
    "Private Finance flagship" to "Tài chính riêng tư nổi bật", "Private Finance Suite" to "Bộ tài chính riêng tư", "Money Studio" to "Công cụ tài chính",
    "Creative Suite flagship" to "Bộ sáng tạo nổi bật", "Creator flagship" to "Công cụ sáng tạo nổi bật", "Decision Suite flagship" to "Bộ quyết định nổi bật",
    "Everyday Tools flagship" to "Công cụ hằng ngày nổi bật", "Light Suite flagship" to "Bộ đèn nổi bật", "Audio Care flagship" to "Chăm sóc âm thanh nổi bật",
    "Connection Care flagship" to "Chăm sóc kết nối nổi bật", "Storage Care flagship" to "Chăm sóc bộ nhớ nổi bật", "Private media utility" to "Tiện ích đa phương tiện riêng tư",
    "A calm two-axis level and quick ruler powered by private on-device readings." to "Thước cân bằng hai trục và thước đo nhanh, xử lý dữ liệu cảm biến riêng tư trên thiết bị.",
    "A gentle breathing coach with clear pacing, accessible motion, and private session goals." to "Hướng dẫn hít thở nhẹ nhàng, nhịp rõ ràng, chuyển động dễ theo dõi và mục tiêu riêng tư.",
    "A spring-smoothed heading from locally filtered motion and magnetic sensors." to "Hướng la bàn được làm mượt từ cảm biến chuyển động và từ trường, xử lý tại máy.",
    "Accurate focus timing, quick presets, private weekly progress, and on-device soundscapes." to "Hẹn giờ tập trung chính xác, mẫu nhanh, tiến độ tuần riêng tư và âm thanh trên thiết bị.",
    "Assign shared items, settle clearly, and save reusable groups without an account." to "Chia món dùng chung, tính tiền rõ ràng và lưu nhóm để dùng lại mà không cần tài khoản.",
    "Build a private wallpaper rotation from local images and apply a fresh look whenever you choose." to "Tạo bộ hình nền riêng tư từ ảnh trên máy và thay đổi bất cứ khi nào bạn muốn.",
    "Capture accurate HEX and RGB colors in real time while every camera frame stays on your device." to "Lấy màu HEX và RGB chính xác theo thời gian thực; mọi khung hình camera đều ở trên thiết bị.",
    "Capture local MP4 video with quality controls and Android's visible consent flow." to "Quay video MP4 trên máy với tùy chọn chất lượng và bước xác nhận rõ ràng của Android.",
    "Capture, crop, reorder, and export clean PDF pages. Pair it with OCR Studio for searchable text." to "Chụp, cắt, sắp xếp và xuất PDF sạch; kết hợp OCR để nhận dạng văn bản.",
    "Current, peak, and rolling sound estimates with no streaming or upload path." to "Ước tính âm lượng hiện tại, đỉnh và trung bình mà không truyền phát hay tải dữ liệu lên mạng.",
    "Fast everyday conversions with zero-latency math, useful categories and complete offline privacy." to "Đổi đơn vị hằng ngày tức thì, danh mục hữu ích và riêng tư hoàn toàn ngoại tuyến.",
    "Keep credentials protected with Android-backed encryption, guarded screenshots and sensitive clipboard cleanup." to "Bảo vệ thông tin đăng nhập bằng mã hóa Android, chặn ảnh chụp và tự xóa clipboard nhạy cảm.",
    "Mix shared charges with item assignments, manage reusable presets locally, and share the final split as plain text." to "Kết hợp phí chung với từng món, quản lý mẫu trên máy và chia sẻ kết quả dạng văn bản.",
    "Receipt capture, monthly budget, category insights and a portable local ledger without accounts or ads." to "Quét hóa đơn, ngân sách tháng, thống kê danh mục và sổ chi tiêu trên máy, không tài khoản hay quảng cáo.",
    "Reliable torch control, emergency SOS and custom Morse patterns, processed entirely on your device." to "Điều khiển đèn pin ổn định, SOS và mã Morse tùy chỉnh, xử lý hoàn toàn trên thiết bị.",
    "Remove location and camera metadata by re-encoding a new share-ready photo locally." to "Xóa vị trí và metadata camera bằng cách tạo ảnh mới sẵn sàng chia sẻ ngay trên máy.",
    "Scan visible media locally, review exact evidence, and approve every deletion yourself." to "Quét nội dung hiển thị trên máy, xem bằng chứng cụ thể và tự xác nhận từng lần xóa.",
    "Timed low-frequency presets designed to help move light moisture from a phone speaker." to "Các mẫu tần số thấp có hẹn giờ giúp đẩy hơi ẩm nhẹ khỏi loa điện thoại.",
    "Turn any list into a smooth, playful offline wheel and make everyday choices without tracking." to "Biến danh sách thành vòng quay ngoại tuyến mượt mà để chọn nhanh mà không theo dõi.",
    "Understand signal quality, nearby networks and connection history with transparent Android permissions." to "Xem chất lượng tín hiệu, mạng lân cận và lịch sử kết nối với quyền Android minh bạch.",
    "Allow Camera for Color Grabber" to "Cho phép camera để lấy màu", "Complete the first field to preview" to "Điền trường đầu tiên để xem trước",
    "Created offline with reliable error correction" to "Tạo ngoại tuyến với khả năng sửa lỗi tin cậy", "Crop selected page directly" to "Cắt trực tiếp trang đã chọn",
    "Fast scans, safe previews, zero ads." to "Quét nhanh, xem trước an toàn, không quảng cáo.", "Save scan history" to "Lưu lịch sử quét",
    "Estimate only. Android microphones vary by device; do not use this result for legal or workplace safety decisions." to "Chỉ là số ước tính. Micro Android khác nhau theo thiết bị; không dùng kết quả này cho pháp lý hoặc an toàn lao động.",
    "Its private check-in history will also be removed from this device." to "Lịch sử đánh dấu riêng tư của thói quen cũng sẽ bị xóa khỏi thiết bị.",
    "Phone DPI can be approximate. Compare the ruler with a known reference and adjust calibration before measuring." to "DPI điện thoại có thể không chính xác. Hãy so với vật chuẩn và hiệu chỉnh trước khi đo.",
    "Play the same familiar audio at a comfortable volume and compare clarity before running another cycle." to "Phát cùng một đoạn âm thanh quen thuộc ở mức vừa phải và so sánh độ rõ trước chu kỳ tiếp theo.",
    "Start at a comfortable volume, keep the speaker facing down, and stop if the sound distorts." to "Bắt đầu với âm lượng vừa phải, hướng loa xuống và dừng nếu âm thanh bị méo.",
    "Turn off for a private session with no QR payload retained." to "Tắt để không lưu nội dung QR trong phiên riêng tư.",
    "Screenshots and Android cloud backup are blocked while this vault is open. Keep a separate secure backup; this local vault should not be your only copy of critical credentials." to "Ảnh chụp màn hình và sao lưu đám mây Android bị chặn khi kho đang mở. Hãy giữ một bản sao lưu an toàn riêng cho thông tin quan trọng.",
    "Metadata removed" to "Đã xóa metadata", "Original protected" to "Đã bảo vệ bản gốc", "Wallpaper Studio" to "Công cụ hình nền", "Wi-Fi Analyzer" to "Phân tích Wi-Fi",
    "Sound Meter" to "Máy đo âm thanh", "Batch" to "Hàng loạt", "Stop Rotation" to "Dừng luân phiên", "Target bearing 0–359°" to "Góc mục tiêu 0–359°",
    "PRIVATE BY DESIGN" to "RIÊNG TƯ NGAY TỪ THIẾT KẾ", "PRIVATE DAILY RHYTHM" to "NHỊP SỐNG RIÊNG TƯ MỖI NGÀY", "DAY STREAK" to "CHUỖI NGÀY",
    "Color HEX copied locally." to "Đã sao chép mã HEX trên máy.", "Media access granted. Scan stays on this device." to "Đã cấp quyền đa phương tiện. Quá trình quét chỉ diễn ra trên thiết bị.",
    "Android removed the files you approved." to "Android đã xóa các tệp bạn xác nhận.", "Approved files were submitted for deletion." to "Đã gửi yêu cầu xóa các tệp bạn xác nhận.",
    "Receipt recognized locally. Review totals and assignments." to "Đã nhận dạng hóa đơn trên máy. Hãy kiểm tra tổng tiền và phân chia.", "Receipt OCR could not read this image." to "OCR hóa đơn không đọc được ảnh này.",
    "Receipt recognized locally. Review the fields before saving." to "Đã nhận dạng hóa đơn trên máy. Hãy kiểm tra các trường trước khi lưu.", "Split summary ready to share." to "Tổng kết chia tiền đã sẵn sàng để chia sẻ.",
    "Wallpaper sources saved locally." to "Đã lưu nguồn hình nền trên máy.", "Applied next wallpaper from local set." to "Đã áp dụng hình nền tiếp theo từ bộ ảnh trên máy.",
    "Wallpaper rotation scheduled." to "Đã lên lịch luân phiên hình nền.", "Wallpaper rotation stopped." to "Đã dừng luân phiên hình nền.",
    "Encrypted PureHub backup saved and fingerprint recorded." to "Đã lưu bản sao PureHub mã hóa và ghi nhận dấu vân tay tệp.", "Backup restored. Reopen tools to refresh their data." to "Đã khôi phục sao lưu. Mở lại công cụ để làm mới dữ liệu.",
    "Backup import failed." to "Nhập bản sao lưu thất bại.", "Backup export failed." to "Xuất bản sao lưu thất bại.", "PDF exported locally." to "Đã xuất PDF trên máy.",
    "Opening exported PDF." to "Đang mở PDF đã xuất.", "PDF ready to share." to "PDF đã sẵn sàng chia sẻ.", "Password copied. Clipboard clears in 30 seconds." to "Đã sao chép mật khẩu. Clipboard sẽ xóa sau 30 giây.",
    "Saved to your private OCR library." to "Đã lưu vào thư viện OCR riêng tư.", "On-device" to "Trên thiết bị",
    "Continue in Doc to PDF" to "Tiếp tục trong Tài liệu sang PDF", "Email" to "Email", "HELP" to "TRỢ GIÚP",
    "Habit, check-ins, expenses, passwords, 2FA accounts and OCR/QR history. AES-256; your passphrase never leaves this device." to "Thói quen, đánh dấu, chi tiêu, mật khẩu, tài khoản 2FA và lịch sử OCR/QR. Mã hóa AES-256; mật khẩu sao lưu không rời khỏi thiết bị.",
    "Support is always voluntary. Community badges may celebrate contributors, but core tools remain available to every user." to "Ủng hộ luôn là tự nguyện. Huy hiệu cộng đồng có thể ghi nhận người đóng góp, nhưng công cụ cốt lõi vẫn dành cho mọi người.",
    "A scan can contain up to 20 pages. Export this document before starting another." to "Một bản quét tối đa 20 trang. Hãy xuất tài liệu trước khi bắt đầu bản mới.",
    "Allow visible media access to review large files and exact duplicate photos or videos." to "Cho phép truy cập nội dung hiển thị để xem tệp lớn và ảnh/video trùng khớp hoàn toàn.",
    "Creating privacy-clean copy on this device..." to "Đang tạo bản sao sạch metadata trên thiết bị...", "No flash unit is available on this device." to "Thiết bị này không có đèn flash.",
    "No readable text found. Try better light or a tighter crop." to "Không tìm thấy chữ dễ đọc. Hãy tăng ánh sáng hoặc cắt sát hơn.", "OCR could not process this image." to "OCR không xử lý được ảnh này.",
    "Opened from your private OCR library." to "Đã mở từ thư viện OCR riêng tư.", "Opening the selected image locally..." to "Đang mở ảnh đã chọn trên thiết bị...",
    "Ready for a new document." to "Sẵn sàng cho tài liệu mới.", "Recognizing text on this device..." to "Đang nhận dạng chữ trên thiết bị...", "SHA-256 checksums calculated locally." to "Đã tính mã kiểm SHA-256 trên thiết bị.",
    "That image could not be opened." to "Không thể mở ảnh đó.", "This library item has text only. Add its original image before sending to Doc to PDF." to "Mục thư viện này chỉ có văn bản. Hãy thêm ảnh gốc trước khi gửi sang Tài liệu sang PDF.",
    "ZIP created locally." to "Đã tạo ZIP trên thiết bị.",
)

private val zhAudit = mapOf(
    "Community flagship" to "旗舰社区", "PureHub belongs to everyone" to "PureHub 属于每个人", "Join the conversation, report issues and shape free, no-ad, open-source tools together." to "参与讨论、报告问题，共同打造免费、无广告的开源工具。",
    "Zen Suite flagship" to "旗舰禅意套件", "Sensor Suite" to "传感器套件", "Document Suite" to "文档套件", "Security Suite flagship" to "旗舰安全套件",
    "Private Finance flagship" to "旗舰私密财务", "Private Finance Suite" to "私密财务套件", "Money Studio" to "财务工具", "Creative Suite flagship" to "旗舰创意套件",
    "Creator flagship" to "旗舰创作工具", "Decision Suite flagship" to "旗舰决策套件", "Everyday Tools flagship" to "旗舰日常工具", "Light Suite flagship" to "旗舰灯光套件",
    "Audio Care flagship" to "旗舰音频护理", "Connection Care flagship" to "旗舰连接护理", "Storage Care flagship" to "旗舰存储护理", "Private media utility" to "私密媒体工具",
    "A calm two-axis level and quick ruler powered by private on-device readings." to "平静的双轴水平仪和快速尺子，读数仅在设备上处理。",
    "A gentle breathing coach with clear pacing, accessible motion, and private session goals." to "节奏清晰、动画舒适且目标私密的温和呼吸指导。",
    "A spring-smoothed heading from locally filtered motion and magnetic sensors." to "通过本机运动和磁场传感器滤波获得平滑方向。",
    "Accurate focus timing, quick presets, private weekly progress, and on-device soundscapes." to "精准专注计时、快捷预设、私密周进度和本机环境音。",
    "Assign shared items, settle clearly, and save reusable groups without an account." to "分配共享项目、清晰结算并保存可复用分组，无需账户。",
    "Build a private wallpaper rotation from local images and apply a fresh look whenever you choose." to "使用本地图片创建私密壁纸轮换，随时更换外观。",
    "Capture accurate HEX and RGB colors in real time while every camera frame stays on your device." to "实时获取准确 HEX 和 RGB 颜色，所有相机画面均留在设备上。",
    "Capture local MP4 video with quality controls and Android's visible consent flow." to "通过 Android 明确授权流程和质量控制录制本地 MP4。",
    "Capture, crop, reorder, and export clean PDF pages. Pair it with OCR Studio for searchable text." to "拍摄、裁剪、排序并导出清晰 PDF，可结合 OCR 识别文字。",
    "Current, peak, and rolling sound estimates with no streaming or upload path." to "估算当前、峰值和滚动声级，不传输或上传。",
    "Fast everyday conversions with zero-latency math, useful categories and complete offline privacy." to "即时日常换算、实用分类和完整离线隐私。",
    "Keep credentials protected with Android-backed encryption, guarded screenshots and sensitive clipboard cleanup." to "通过 Android 加密、截图保护和敏感剪贴板清理来保护凭据。",
    "Mix shared charges with item assignments, manage reusable presets locally, and share the final split as plain text." to "组合共享费用和项目分配，在本机管理预设并以文本分享结果。",
    "Receipt capture, monthly budget, category insights and a portable local ledger without accounts or ads." to "扫描收据、月度预算、分类分析和本地账本，无账户、无广告。",
    "Reliable torch control, emergency SOS and custom Morse patterns, processed entirely on your device." to "可靠手电筒、紧急 SOS 和自定义摩尔斯模式，完全在设备上处理。",
    "Remove location and camera metadata by re-encoding a new share-ready photo locally." to "在本机重新编码可分享照片，移除位置和相机元数据。",
    "Scan visible media locally, review exact evidence, and approve every deletion yourself." to "本机扫描可见媒体，查看明确依据并亲自批准每次删除。",
    "Timed low-frequency presets designed to help move light moisture from a phone speaker." to "定时低频预设，帮助排出手机扬声器中的少量水分。",
    "Turn any list into a smooth, playful offline wheel and make everyday choices without tracking." to "将列表变成流畅有趣的离线转盘，不被跟踪地做出日常选择。",
    "Understand signal quality, nearby networks and connection history with transparent Android permissions." to "通过透明 Android 权限了解信号质量、附近网络和连接历史。",
    "Allow Camera for Color Grabber" to "允许相机取色", "Complete the first field to preview" to "填写第一个字段以预览", "Created offline with reliable error correction" to "离线创建并具备可靠纠错",
    "Crop selected page directly" to "直接裁剪所选页面", "Fast scans, safe previews, zero ads." to "快速扫描、安全预览、零广告。", "Save scan history" to "保存扫描历史",
    "Estimate only. Android microphones vary by device; do not use this result for legal or workplace safety decisions." to "仅为估算值。不同 Android 设备的麦克风存在差异，请勿用于法律或工作场所安全判断。",
    "Its private check-in history will also be removed from this device." to "其私密打卡历史也会从此设备移除。", "Phone DPI can be approximate. Compare the ruler with a known reference and adjust calibration before measuring." to "手机 DPI 可能不精确，请先用已知参照物校准再测量。",
    "Play the same familiar audio at a comfortable volume and compare clarity before running another cycle." to "以舒适音量播放同一段熟悉音频，比较清晰度后再运行下一周期。",
    "Start at a comfortable volume, keep the speaker facing down, and stop if the sound distorts." to "从舒适音量开始，让扬声器朝下；若声音失真请停止。", "Turn off for a private session with no QR payload retained." to "关闭后将进行不保留 QR 内容的私密会话。",
    "Metadata removed" to "元数据已移除", "Original protected" to "原图已保护", "Wallpaper Studio" to "壁纸工具", "Wi-Fi Analyzer" to "Wi-Fi 分析", "Sound Meter" to "声级计",
    "Batch" to "批量", "Stop Rotation" to "停止轮换", "Target bearing 0–359°" to "目标方位角 0–359°", "PRIVATE BY DESIGN" to "隐私源于设计", "PRIVATE DAILY RHYTHM" to "私密日常节奏", "DAY STREAK" to "连续天数",
    "Color HEX copied locally." to "已在本机复制 HEX。", "Media access granted. Scan stays on this device." to "已授权媒体访问，扫描仅在设备上进行。", "Android removed the files you approved." to "Android 已删除你批准的文件。",
    "Approved files were submitted for deletion." to "已提交删除所批准文件。", "Receipt recognized locally. Review totals and assignments." to "已在本机识别收据，请检查总额和分配。", "Receipt OCR could not read this image." to "收据 OCR 无法读取此图片。",
    "Receipt recognized locally. Review the fields before saving." to "已在本机识别收据，保存前请检查字段。", "Split summary ready to share." to "分摊汇总已可分享。", "Wallpaper sources saved locally." to "壁纸来源已保存在本机。",
    "Applied next wallpaper from local set." to "已应用本地集合中的下一张壁纸。", "Wallpaper rotation scheduled." to "已安排壁纸轮换。", "Wallpaper rotation stopped." to "壁纸轮换已停止。",
    "Encrypted PureHub backup saved and fingerprint recorded." to "PureHub 加密备份已保存并记录文件指纹。", "Backup restored. Reopen tools to refresh their data." to "备份已恢复，请重新打开工具以刷新数据。",
    "Backup import failed." to "导入备份失败。", "Backup export failed." to "导出备份失败。", "PDF exported locally." to "PDF 已导出到本机。", "Opening exported PDF." to "正在打开导出的 PDF。",
    "PDF ready to share." to "PDF 已可分享。", "Password copied. Clipboard clears in 30 seconds." to "密码已复制，剪贴板将在 30 秒后清除。", "Saved to your private OCR library." to "已保存到私密 OCR 资料库。", "On-device" to "设备端",
    "Continue in Doc to PDF" to "继续到文档转 PDF", "Email" to "电子邮件", "HELP" to "帮助",
    "Habit, check-ins, expenses, passwords, 2FA accounts and OCR/QR history. AES-256; your passphrase never leaves this device." to "备份习惯、打卡、支出、密码、2FA 账户和 OCR/QR 历史。采用 AES-256，加密口令不会离开设备。",
    "Support is always voluntary. Community badges may celebrate contributors, but core tools remain available to every user." to "支持始终自愿。社区徽章可表彰贡献者，但核心工具对所有用户开放。",
    "A scan can contain up to 20 pages. Export this document before starting another." to "一次扫描最多 20 页，请先导出当前文档再开始新的扫描。",
    "Allow visible media access to review large files and exact duplicate photos or videos." to "允许访问可见媒体，以检查大文件和完全重复的照片或视频。",
    "Creating privacy-clean copy on this device..." to "正在设备上创建清除隐私信息的副本...", "No flash unit is available on this device." to "此设备没有闪光灯。",
    "No readable text found. Try better light or a tighter crop." to "未找到可读文字，请改善光线或缩小裁剪范围。", "OCR could not process this image." to "OCR 无法处理此图片。",
    "Opened from your private OCR library." to "已从私密 OCR 资料库打开。", "Opening the selected image locally..." to "正在本机打开所选图片...", "Ready for a new document." to "已准备好新文档。",
    "Recognizing text on this device..." to "正在设备上识别文字...", "SHA-256 checksums calculated locally." to "已在本机计算 SHA-256 校验值。", "That image could not be opened." to "无法打开该图片。",
    "This library item has text only. Add its original image before sending to Doc to PDF." to "此资料库项目仅包含文字，请添加原图后再发送到文档转 PDF。", "ZIP created locally." to "ZIP 已在本机创建。",
)

private val viAll = vi + viMore + viAudit
private val zhAll = zh + zhMore + zhAudit

fun translateUiText(text: String, language: AppLanguage): String {
    if (language == AppLanguage.English || text.isBlank()) return text
    val dictionary = if (language == AppLanguage.Vietnamese) viAll else zhAll
    dictionary[text]?.let { return it }
    val trimmed = text.trim()
    dictionary[trimmed]?.let { return text.replace(trimmed, it) }
    return dynamicTranslation(text, language)
}

private fun dynamicTranslation(text: String, language: AppLanguage): String {
    patternTranslation(text, language)?.let { return it }
    val lunarText = if (text.contains("Year ") && text.contains("Month ") && text.contains("Day ")) {
        val names = if (language == AppLanguage.Vietnamese) listOf(
            "Giap" to "Giáp", "At" to "Ất", "Binh" to "Bính", "Dinh" to "Đinh", "Mau" to "Mậu", "Ky" to "Kỷ", "Canh" to "Canh", "Tan" to "Tân", "Nham" to "Nhâm", "Quy" to "Quý",
            "Ty." to "Tỵ", "Ty" to "Tý", "Suu" to "Sửu", "Dan" to "Dần", "Mao" to "Mão", "Thin" to "Thìn", "Ngo" to "Ngọ", "Mui" to "Mùi", "Than" to "Thân", "Dau" to "Dậu", "Tuat" to "Tuất", "Hoi" to "Hợi",
        ) else listOf(
            "Giap" to "甲", "At" to "乙", "Binh" to "丙", "Dinh" to "丁", "Mau" to "戊", "Ky" to "己", "Canh" to "庚", "Tan" to "辛", "Nham" to "壬", "Quy" to "癸",
            "Ty." to "巳", "Ty" to "子", "Suu" to "丑", "Dan" to "寅", "Mao" to "卯", "Thin" to "辰", "Ngo" to "午", "Mui" to "未", "Than" to "申", "Dau" to "酉", "Tuat" to "戌", "Hoi" to "亥",
        )
        names.fold(text) { value, (source, target) -> value.replace(source, target) }
    } else text
    val replacements = if (language == AppLanguage.Vietnamese) listOf(
        " reviewed files?" to " tệp đã xem?", " reviewed" to " đã xem", "Remove " to "Xóa ", "This can free " to "Có thể giải phóng ",
        ". Android may show one more confirmation. PureHub never selects personal files silently." to ". Android có thể yêu cầu xác nhận thêm một lần. PureHub không bao giờ âm thầm chọn tệp cá nhân.",
        " byte-for-byte matches" to " tệp trùng khớp hoàn toàn", "Review " to "Xem lại ", " after keeping the newest" to " sau khi giữ bản mới nhất",
        "Lunar " to "Âm lịch ", "Year " to "Năm ", "Month " to "Tháng ", "Day " to "Ngày ", " (Leap)" to " (nhuận)",
        "Active" to "Đang dùng", "Selected" to "Đã chọn", "Assigned:" to "Đã chia:", "Shared:" to "Dùng chung:",
        "Updated " to "Cập nhật ", "Target " to "Mục tiêu ", "Accuracy:" to "Độ chính xác:", "Tolerance " to "Sai số ",
        "Calibration offset " to "Độ lệch hiệu chỉnh ", "Recent " to "Gần đây ", "No photo selected" to "Chưa chọn ảnh", "Photo selected" to "Đã chọn ảnh",
        " tone" to " · sắc độ", " · encrypted locally" to " · mã hóa trên máy", " · processed locally" to " · xử lý trên máy",
        " nearby" to " lân cận", "strongest " to "mạnh nhất ", "Edge crop " to "Cắt mép ", "Rotate " to "Xoay ",
        "Ruler calibration " to "Hiệu chỉnh thước ", "Ruler " to "Thước ", "Vault health " to "Sức khỏe kho ",
        "Shared remainder:" to "Phần còn lại dùng chung:", "Extras:" to "Phụ phí:", "Person " to "Người ", "Total:" to "Tổng:",
        "Could not create ZIP:" to "Không thể tạo ZIP:",
    ) else listOf(
        " reviewed files?" to " 个已检查文件？", " reviewed" to " 个已检查", "Remove " to "删除 ", "This can free " to "可释放 ",
        ". Android may show one more confirmation. PureHub never selects personal files silently." to "。Android 可能会再次要求确认。PureHub 绝不会静默选择个人文件。",
        " byte-for-byte matches" to " 个完全相同文件", "Review " to "检查 ", " after keeping the newest" to "（保留最新文件后）",
        "Lunar " to "农历 ", "Year " to "年 ", "Month " to "月 ", "Day " to "日 ", " (Leap)" to "（闰月）",
        "Active" to "使用中", "Selected" to "已选择", "Assigned:" to "已分配：", "Shared:" to "共享：",
        "Updated " to "更新于 ", "Target " to "目标 ", "Accuracy:" to "精度：", "Tolerance " to "容差 ",
        "Calibration offset " to "校准偏移 ", "Recent " to "最近 ", "No photo selected" to "未选择照片", "Photo selected" to "已选择照片",
        " tone" to " · 色调", " · encrypted locally" to " · 本机加密", " · processed locally" to " · 本机处理",
        " nearby" to " 个附近网络", "strongest " to "最强 ", "Edge crop " to "边缘裁剪 ", "Rotate " to "旋转 ",
        "Ruler calibration " to "尺子校准 ", "Ruler " to "尺子 ", "Vault health " to "保险库健康度 ",
        "Shared remainder:" to "共享余款：", "Extras:" to "额外费用：", "Person " to "人员 ", "Total:" to "总计：", "Could not create ZIP:" to "无法创建 ZIP：",
    )
    var translated = lunarText
    replacements.forEach { (source, target) -> translated = translated.replace(source, target) }
    return translated
}

private fun patternTranslation(text: String, language: AppLanguage): String? {
    fun match(pattern: String) = Regex(pattern).matchEntire(text)?.groupValues
    val vi = language == AppLanguage.Vietnamese
    match("(\\d+)% of (\\d+)-minute goal")?.let { return if (vi) "${it[1]}% mục tiêu ${it[2]} phút" else "${it[2]} 分钟目标的 ${it[1]}%" }
    match("(\\d+) min")?.let { return if (vi) "${it[1]} phút" else "${it[1]} 分钟" }
    match("(\\d+) sec")?.let { return if (vi) "${it[1]} giây" else "${it[1]} 秒" }
    match("(\\d+) image\\(s\\) selected")?.let { return if (vi) "Đã chọn ${it[1]} ảnh" else "已选择 ${it[1]} 张图片" }
    match("(\\d+) pages?")?.let { return if (vi) "${it[1]} trang" else "${it[1]} 页" }
    match("(\\d+) words")?.let { return if (vi) "${it[1]} từ" else "${it[1]} 个词" }
    match("(\\d+) one-second samples")?.let { return if (vi) "${it[1]} mẫu mỗi giây" else "${it[1]} 个一秒采样" }
    match("(\\d+) page\\(s\\) captured privately\\. Review the text before export\\.")?.let { return if (vi) "Đã chụp riêng tư ${it[1]} trang. Hãy kiểm tra văn bản trước khi xuất." else "已私密拍摄 ${it[1]} 页，导出前请检查文字。" }
    match("(\\d+) searchable page\\(s\\) sent to Doc to PDF\\.")?.let { return if (vi) "Đã gửi ${it[1]} trang có thể tìm kiếm sang Tài liệu sang PDF." else "已将 ${it[1]} 个可搜索页面发送到文档转 PDF。" }
    match("Document Suite is ready with (\\d+) OCR page\\(s\\)\\.")?.let { return if (vi) "Bộ tài liệu đã sẵn sàng với ${it[1]} trang OCR." else "文档套件已准备好 ${it[1]} 个 OCR 页面。" }
    match("Preset \"(.+)\" saved locally\\.")?.let { return if (vi) "Đã lưu mẫu \"${it[1]}\" trên máy." else "预设“${it[1]}”已保存在本机。" }
    match("Preset \"(.+)\" deleted\\.")?.let { return if (vi) "Đã xóa mẫu \"${it[1]}\"." else "预设“${it[1]}”已删除。" }
    match("Preset renamed to \"(.+)\"\\.")?.let { return if (vi) "Đã đổi tên mẫu thành \"${it[1]}\"." else "预设已重命名为“${it[1]}”。" }
    match("(\\d+) entries · (\\d+) weak · (\\d+) reused")?.let { return if (vi) "${it[1]} mục · ${it[2]} yếu · ${it[3]} dùng lại" else "${it[1]} 项 · ${it[2]} 个弱密码 · ${it[3]} 个重复" }
    match("(\\d+) complete cycles · (.+) elapsed · (\\d+) saved sessions")?.let { return if (vi) "${it[1]} chu kỳ hoàn tất · đã chạy ${it[2]} · ${it[3]} phiên đã lưu" else "完成 ${it[1]} 个周期 · 已用 ${it[2]} · 保存 ${it[3]} 个会话" }
    match("This week: (\\d+) sessions · (\\d+) focused minutes")?.let { return if (vi) "Tuần này: ${it[1]} phiên · ${it[2]} phút tập trung" else "本周：${it[1]} 次 · 专注 ${it[2]} 分钟" }
    match("Best streak (\\d+) days?")?.let { return if (vi) "Chuỗi tốt nhất ${it[1]} ngày" else "最佳连续 ${it[1]} 天" }
    match("Pitch (-?[\\d.]+) deg")?.let { return if (vi) "Góc dọc ${it[1]}°" else "俯仰角 ${it[1]}°" }
    match("Roll (-?[\\d.]+) deg")?.let { return if (vi) "Góc ngang ${it[1]}°" else "横滚角 ${it[1]}°" }
    match("Frequency ([\\d.]+) (Hz|MHz)")?.let { return if (vi) "Tần số ${it[1]} ${it[2]}" else "频率 ${it[1]} ${it[2]}" }
    match("Volume (\\d+)%")?.let { return if (vi) "Âm lượng ${it[1]}%" else "音量 ${it[1]}%" }
    match("Estimated peak ~(-?[\\d.]+) dB")?.let { return if (vi) "Đỉnh ước tính ~${it[1]} dB" else "估算峰值 ~${it[1]} dB" }
    match("Rolling average ~(-?[\\d.]+) dB · last (\\d+)s")?.let { return if (vi) "Trung bình ~${it[1]} dB · ${it[2]} giây gần nhất" else "滚动平均 ~${it[1]} dB · 最近 ${it[2]} 秒" }
    match("Min (-?[\\d.]+) · Avg (-?[\\d.]+) · Max (-?[\\d.]+) dB")?.let { return if (vi) "Thấp ${it[1]} · TB ${it[2]} · Cao ${it[3]} dB" else "最低 ${it[1]} · 平均 ${it[2]} · 最高 ${it[3]} dB" }
    match("Target ([\\d.]+)° · deviation ([\\d.]+)°")?.let { return if (vi) "Mục tiêu ${it[1]}° · lệch ${it[2]}°" else "目标 ${it[1]}° · 偏差 ${it[2]}°" }
    match("Heading: (\\d+) deg (.+)")?.let { return if (vi) "Hướng: ${it[1]}° ${it[2]}" else "方向：${it[1]}° ${it[2]}" }
    match("Band (.+)")?.let { return if (vi) "Băng tần ${it[1]}" else "频段 ${it[1]}" }
    match("Delete (.+)\\?")?.let { return if (vi) "Xóa ${it[1]}?" else "删除 ${it[1]}？" }
    match("Link (.+) Mbps")?.let { return if (vi) "Liên kết ${it[1]} Mbps" else "连接 ${it[1]} Mbps" }
    match("P(\\d+)")?.let { return if (vi) "Người ${it[1]}" else "人员 ${it[1]}" }
    match("Rotate (-?[\\d.]+)°")?.let { return if (vi) "Xoay ${it[1]}°" else "旋转 ${it[1]}°" }
    return null
}

@Composable
fun LocalizedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
) {
    Text(
        text = translateUiText(text, LocalAppLanguage.current),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style,
    )
}

@Composable
fun LocalizedText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, androidx.compose.foundation.text.InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
        onTextLayout = onTextLayout,
        style = style,
    )
}
