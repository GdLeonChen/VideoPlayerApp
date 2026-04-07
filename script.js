class VideoPlayer {
    constructor() {
        this.videoElement = document.getElementById('videoPlayer');
        this.folderSelectBtn = document.getElementById('folderSelect');
        this.networkSelectBtn = document.getElementById('networkSelect');
        this.fileInput = document.getElementById('fileInput');
        this.videoInfo = document.getElementById('videoInfo');
        this.playerContainer = document.getElementById('playerContainer');
        
        this.videoFiles = [];
        this.currentIndex = 0;
        this.startY = 0;
        this.endY = 0;
        
        this.init();
    }
    
    init() {
        this.setupEventListeners();
    }
    
    setupEventListeners() {
        // 文件夹选择
        this.folderSelectBtn.addEventListener('click', () => {
            this.fileInput.click();
        });
        
        // 文件选择变化
        this.fileInput.addEventListener('change', (e) => {
            this.handleFileSelect(e);
        });
        
        // 局域网文件选择
        this.networkSelectBtn.addEventListener('click', () => {
            this.handleNetworkSelect();
        });
        
        // 视频播放结束事件
        this.videoElement.addEventListener('ended', () => {
            this.playNextVideo();
        });
        
        // 触摸事件 - 下滑切换视频
        this.playerContainer.addEventListener('touchstart', (e) => {
            this.startY = e.touches[0].clientY;
        });
        
        this.playerContainer.addEventListener('touchend', (e) => {
            this.endY = e.changedTouches[0].clientY;
            this.handleSwipe();
        });
    }
    
    handleFileSelect(e) {
        const files = Array.from(e.target.files);
        this.videoFiles = files.filter(file => this.isVideoFile(file.name));
        
        if (this.videoFiles.length > 0) {
            this.currentIndex = 0;
            this.playVideo(this.videoFiles[this.currentIndex]);
        } else {
            this.videoInfo.textContent = '未找到视频文件';
        }
    }
    
    handleNetworkSelect() {
        const networkPath = prompt('请输入局域网视频路径 (如: http://192.168.1.100/videos/)');
        if (networkPath) {
            this.videoInfo.textContent = '请确保您的设备可以访问该局域网路径';
            // 这里可以添加局域网文件扫描逻辑
        }
    }
    
    isVideoFile(filename) {
        const videoExtensions = ['.mp4', '.avi', '.mov', '.wmv', '.flv', '.mkv', '.webm'];
        const extension = filename.toLowerCase().substring(filename.lastIndexOf('.'));
        return videoExtensions.includes(extension);
    }
    
    playVideo(file) {
        const videoUrl = URL.createObjectURL(file);
        this.videoElement.src = videoUrl;
        this.videoElement.play().catch(error => {
            console.error('播放失败:', error);
        });
        this.videoInfo.textContent = file.name;
    }
    
    playNextVideo() {
        if (this.videoFiles.length > 0) {
            this.currentIndex = (this.currentIndex + 1) % this.videoFiles.length;
            this.playVideo(this.videoFiles[this.currentIndex]);
        }
    }
    
    handleSwipe() {
        const swipeThreshold = 50;
        if (this.startY - this.endY > swipeThreshold) {
            // 下滑操作
            this.playNextVideo();
        }
    }
}

// 初始化播放器
document.addEventListener('DOMContentLoaded', () => {
    new VideoPlayer();
});