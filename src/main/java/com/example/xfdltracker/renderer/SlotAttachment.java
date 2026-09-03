package com.example.xfdltracker.renderer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * {@code (parentFamily, slot)} 조합마다 composed 자식 fragment를 부모에 어떻게 끼워 넣는지
 * 표현하는 catalog-confirmed 규칙. 새 class/token을 발명하지 않는다. {@link #lookup}이 null이면
 * {@link CompositionRenderer}는 UNSUPPORTED_SLOT_ATTACHMENT로 fail-closed한다(추측 fallback 없음).
 */
final class SlotAttachment {

    private static final String NS_XF = "http://www.w3.org/2002/xforms";

    /** {@code SPLIT_LAYOUT.columns} -- 각 자식을 {@code xf:group.ly_column}으로 감싸 lybox root에
     * Plan edge 순서 그대로 append한다. decorative class(col_N 등)는 범위 밖이라 붙이지 않는다. */
    private static final SlotAttachment SPLIT_LAYOUT_COLUMNS = new SlotAttachment("ly_column", false);

    /**
     * {@code TAB_CONTROL.panes}는 generic wrapper를 만들지 않는다 -- {@code w2:content}가 이미
     * 정확한 page container이므로 child root를 그 안에 직접 append한다. 대상 Element는 호출자가
     * pageOrdinal로 조회해 {@link #attachToPage}에 넘긴다.
     */
    private static final SlotAttachment TAB_CONTROL_PANES = new SlotAttachment(null, true);

    private final String wrapperClass;
    private final boolean pageOrdinalAware;

    private SlotAttachment(String wrapperClass, boolean pageOrdinalAware) {
        this.wrapperClass = wrapperClass;
        this.pageOrdinalAware = pageOrdinalAware;
    }

    static SlotAttachment lookup(String parentFamily, String slot) {
        if ("SPLIT_LAYOUT".equals(parentFamily) && "columns".equals(slot)) {
            return SPLIT_LAYOUT_COLUMNS;
        }
        if ("TAB_CONTROL".equals(parentFamily) && "panes".equals(slot)) {
            return TAB_CONTROL_PANES;
        }
        return null;
    }

    /** true면 {@link #attach} 대신 {@link #attachToPage}로 page-specific 위치를 resolve해야 한다. */
    boolean isPageOrdinalAware() { return pageOrdinalAware; }

    void attach(Document doc, Element parentElement, Element childElement) {
        if (pageOrdinalAware) {
            throw new IllegalStateException(
                    "slot_attachment: this attachment is page-ordinal-aware -- use attachToPage(...), not attach(...)");
        }
        Element wrapper = doc.createElementNS(NS_XF, "xf:group");
        wrapper.setAttribute("class", wrapperClass);
        wrapper.appendChild(childElement);
        parentElement.appendChild(wrapper);
    }

    /** {@code TAB_CONTROL.panes} 전용 -- 호출자가 조회한 {@code w2:content} 안에 wrapper 없이 직접 append한다. */
    void attachToPage(Element childElement, Element pageContentElement) {
        if (!pageOrdinalAware) {
            throw new IllegalStateException(
                    "slot_attachment: this attachment is not page-ordinal-aware -- use attach(...), not attachToPage(...)");
        }
        pageContentElement.appendChild(childElement);
    }
}
