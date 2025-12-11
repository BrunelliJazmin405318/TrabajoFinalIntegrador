// js/main.js
document.addEventListener('DOMContentLoaded', () => {
    const navToggle = document.querySelector('.nav-toggle');
    const navMain   = document.querySelector('.nav-main');

    // ===== Menú mobile =====
    if (navToggle && navMain) {
        navToggle.addEventListener('click', () => {
            navMain.classList.toggle('nav-open');
        });

        // Cerrar el menú cuando hago click en un link
        navMain.querySelectorAll('.nav-link').forEach(link => {
            link.addEventListener('click', () => {
                navMain.classList.remove('nav-open');
            });
        });
    }

    // ===== Submenús (ej: Soluciones) =====
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        const toggle  = item.querySelector('.nav-link-toggle');
        const submenu = item.querySelector('.submenu');

        if (!toggle || !submenu) return;

        toggle.addEventListener('click', (e) => {
            e.preventDefault();

            const yaAbierto = item.classList.contains('submenu-open');

            // cierro otros abiertos
            navItems.forEach(i => i.classList.remove('submenu-open'));

            // si este no estaba abierto, lo abro
            if (!yaAbierto) {
                item.classList.add('submenu-open');
            }
        });
    });

    // Cerrar submenú haciendo click fuera
    document.addEventListener('click', (e) => {
        const abierto = document.querySelector('.nav-item.submenu-open');
        if (!abierto) return;

        if (!abierto.contains(e.target)) {
            abierto.classList.remove('submenu-open');
        }
    });
});