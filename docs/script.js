/**
 * CandyRush User Manual - Language Switcher
 */

// Language switching functionality
document.addEventListener('DOMContentLoaded', function() {
    // Get elements
    const langJaBtn = document.getElementById('lang-ja');
    const langEnBtn = document.getElementById('lang-en');
    const contentJa = document.getElementById('content-ja');
    const contentEn = document.getElementById('content-en');

    // Load saved language preference or default to Japanese
    const savedLang = localStorage.getItem('candyrush-lang') || 'ja';
    setLanguage(savedLang);

    // Add click event listeners
    langJaBtn.addEventListener('click', function() {
        setLanguage('ja');
    });

    langEnBtn.addEventListener('click', function() {
        setLanguage('en');
    });

    /**
     * Set the active language
     * @param {string} lang - Language code ('ja' or 'en')
     */
    function setLanguage(lang) {
        if (lang === 'ja') {
            // Show Japanese content
            contentJa.classList.add('active');
            contentEn.classList.remove('active');

            // Update button states
            langJaBtn.classList.add('active');
            langEnBtn.classList.remove('active');

            // Update HTML lang attribute
            document.documentElement.lang = 'ja';
        } else {
            // Show English content
            contentEn.classList.add('active');
            contentJa.classList.remove('active');

            // Update button states
            langEnBtn.classList.add('active');
            langJaBtn.classList.remove('active');

            // Update HTML lang attribute
            document.documentElement.lang = 'en';
        }

        // Save preference to localStorage
        localStorage.setItem('candyrush-lang', lang);

        // Scroll to top when switching languages
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    }

    // Keyboard shortcut: Alt+J for Japanese, Alt+E for English
    document.addEventListener('keydown', function(e) {
        if (e.altKey) {
            if (e.key === 'j' || e.key === 'J') {
                e.preventDefault();
                setLanguage('ja');
            } else if (e.key === 'e' || e.key === 'E') {
                e.preventDefault();
                setLanguage('en');
            }
        }
    });

    // Add smooth scroll behavior to all anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function(e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });

    // Add copy functionality to code elements
    document.querySelectorAll('code').forEach(codeBlock => {
        codeBlock.style.cursor = 'pointer';
        codeBlock.title = 'Click to copy';

        codeBlock.addEventListener('click', function() {
            const text = this.textContent;
            navigator.clipboard.writeText(text).then(() => {
                // Visual feedback
                const originalText = this.textContent;
                const originalBg = this.style.backgroundColor;

                this.style.backgroundColor = '#4ecdc4';
                this.style.color = '#fff';
                this.textContent = '✓ Copied!';

                setTimeout(() => {
                    this.style.backgroundColor = originalBg;
                    this.style.color = '';
                    this.textContent = originalText;
                }, 1000);
            }).catch(err => {
                console.error('Failed to copy:', err);
            });
        });
    });

    // Add scroll-to-top button
    const scrollTopBtn = createScrollTopButton();
    document.body.appendChild(scrollTopBtn);

    // Show/hide scroll-to-top button based on scroll position
    window.addEventListener('scroll', function() {
        if (window.pageYOffset > 300) {
            scrollTopBtn.classList.add('visible');
        } else {
            scrollTopBtn.classList.remove('visible');
        }
    });

    /**
     * Create scroll-to-top button
     * @returns {HTMLElement} Button element
     */
    function createScrollTopButton() {
        const button = document.createElement('button');
        button.className = 'scroll-top-btn';
        button.innerHTML = '↑';
        button.setAttribute('aria-label', 'Scroll to top');

        button.addEventListener('click', function() {
            window.scrollTo({
                top: 0,
                behavior: 'smooth'
            });
        });

        // Add styles
        const style = document.createElement('style');
        style.textContent = `
            .scroll-top-btn {
                position: fixed;
                bottom: 30px;
                right: 30px;
                width: 50px;
                height: 50px;
                background: linear-gradient(135deg, #ff6b6b, #ee5a6f);
                color: white;
                border: none;
                border-radius: 50%;
                font-size: 24px;
                cursor: pointer;
                opacity: 0;
                visibility: hidden;
                transition: all 0.3s ease;
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
                z-index: 1000;
            }

            .scroll-top-btn.visible {
                opacity: 1;
                visibility: visible;
            }

            .scroll-top-btn:hover {
                transform: translateY(-5px);
                box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
            }

            .scroll-top-btn:active {
                transform: translateY(-2px);
            }

            @media (max-width: 768px) {
                .scroll-top-btn {
                    bottom: 20px;
                    right: 20px;
                    width: 45px;
                    height: 45px;
                    font-size: 20px;
                }
            }
        `;
        document.head.appendChild(style);

        return button;
    }

    // Add table of contents navigation (optional enhancement)
    addTableOfContents();

    /**
     * Add a floating table of contents for easier navigation
     */
    function addTableOfContents() {
        // This is a placeholder for future enhancement
        // Could add a collapsible TOC sidebar based on h2 headings
        console.log('Table of contents feature ready for implementation');
    }

    // Analytics tracking (if needed in the future)
    function trackLanguageSwitch(lang) {
        // Placeholder for analytics
        console.log('Language switched to:', lang);
    }

    // Add visual feedback for interactive elements
    document.querySelectorAll('.point-method, .tips-list li, table tbody tr').forEach(element => {
        element.addEventListener('mouseenter', function() {
            this.style.transition = 'all 0.3s ease';
        });
    });

    // Print-friendly version trigger
    const printStyles = `
        @media print {
            .language-switcher,
            .scroll-top-btn,
            footer .github-link {
                display: none !important;
            }
        }
    `;
    const printStyleSheet = document.createElement('style');
    printStyleSheet.textContent = printStyles;
    document.head.appendChild(printStyleSheet);

    console.log('CandyRush User Manual loaded successfully! 🍬');
});
