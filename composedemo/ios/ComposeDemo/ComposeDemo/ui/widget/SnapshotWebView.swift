import UIKit
import WebKit
import ComposeDemoFramework

enum RenderError: Error {
    case invalidUri
    case openStreamFailed
}

class SnapshotWebView: UIView, ISnapshotWebView {
    let webView: WKWebView = WKWebView(frame: .zero)

    override init(frame: CGRect = .zero) {
        super.init(frame: frame)
        setupConfig()
        setupWebView()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupConfig()
        setupWebView()
    }
    
    // 对齐 Android settings
    private func setupConfig() {
        let config = webView.configuration
        config.preferences.javaScriptEnabled = true
//        config.preferences.allowsMixedContent = true
        if #available(iOS 14.0, *) {
//            config.preferences.isFileAccessFromFileURLsAllowed = true
        }
        // 没有 allowUniversalAccessFromFileURLs 原生开关（iOS限制）
        // defaultTextEncodingName：WKWebView 自动识别UTF-8，不需要单独设置
    }
    
    private func setupWebView() {
        addSubview(webView)
        webView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            webView.leadingAnchor.constraint(equalTo: leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: trailingAnchor),
            webView.topAnchor.constraint(equalTo: topAnchor),
            webView.bottomAnchor.constraint(equalTo: bottomAnchor),
        ])
    }
    
    /// 对应 Android initForSnapShot
    func initForSnapShot(width: Int?, height: Int?, enableZoom: Bool = false) {
        if let w = width, let h = height {
            // 非空：固定尺寸（离屏快照模式）
            self.frame = CGRect(x: 0, y: 0, width: CGFloat(w), height: CGFloat(h))
            webView.frame = self.bounds
            // 移除约束，避免和frame冲突
            webView.removeConstraints(webView.constraints)
        }
//        webView.scrollView.isZoomEnabled = enableZoom
        webView.scrollView.showsHorizontalScrollIndicator = false
        webView.scrollView.showsVerticalScrollIndicator = false
        // useWideViewPort / loadWithOverviewMode：WKWebView 默认行为接近，meta viewport控制
    }
    
    /// 对应 suspendLoadUri
    func loadFile(filePath: NSString) {
        // NSString -> URL
        let fileUrl = URL(fileURLWithPath: filePath as String)
        // 父目录，授予读取权限
        let accessDir = fileUrl.deletingLastPathComponent()
        
        webView.loadFileURL(fileUrl, allowingReadAccessTo: accessDir)
    }
    
    func loadFile(filePath: String) {
        // NSString -> URL
        let fileUrl = URL(fileURLWithPath: filePath)
        // 父目录，授予读取权限
        let accessDir = fileUrl.deletingLastPathComponent()
        
        webView.loadFileURL(fileUrl, allowingReadAccessTo: accessDir)
    }
    
    /// 加载失败展示错误页面
    func loadError(message: String) {
        let errHtml = "<h1>加载失败</h1><p>原因：\(message)</p>"
        webView.loadHTMLString(errHtml, baseURL: nil)
    }
    
    func getView() -> UIView {
            return self
        }

    func doInitForSnapShot(width: NSNumber?, height: NSNumber?, enabled_: Bool) {
        let w: Int? = width?.intValue
        let h: Int? = height?.intValue
        initForSnapShot(width: w, height: h, enableZoom: enabled_)
    }

    func tackSnapShot(completionHandler: @escaping (UIImage?, NSError?) -> Void) {
        Task {
            do {
                let img = try await webView.takeSnapshot()
                completionHandler(img, nil)
            } catch {
                completionHandler(nil, error as NSError)
            }
        }
    }
    func tackSnapShot() async throws -> UIImage {
        return try await webView.takeSnapshot()
    }
}

// 封装 WKWebView 快照 async 扩展
extension WKWebView {
    func takeSnapshot() async throws -> UIImage {
        try await withCheckedThrowingContinuation { cont in
            let config = WKSnapshotConfiguration()
            config.rect = self.bounds
            self.takeSnapshot(with: config) { image, err in
                if let err = err {
                    cont.resume(throwing: err)
                } else if let img = image {
                    cont.resume(returning: img)
                } else {
                    cont.resume(throwing: NSError(domain: "CommonWebView", code: -1, userInfo: [NSLocalizedDescriptionKey: "快照为空"]))
                }
            }
        }
    }
}

class SnapshotWebViewFactoryImpl: SnapshotWebViewFactory {
    func doNewInstance() -> ISnapshotWebView {
        return SnapshotWebView()
    }
}
