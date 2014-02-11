package fr.wseduc.webutils;

public abstract class Either<A, B> {

	public abstract boolean isLeft();
	public abstract boolean isRight();
	public abstract Left<A, B> left();
	public abstract Right<A, B> right();

	private Either() {}

	interface Function<T> {
		void apply(T x);
	}

	public final static class Left<L, R> extends Either<L, R>  {
		private final L leftValue;

		public Left(L l) {
			leftValue = l;
		}

		public boolean isRight() {
			return false;
		}

		public boolean isLeft() {
			return true;
		}

		public Right<L, R> right() {
			return null;
		}

		public Left<L, R> left() {
			return this;
		}

		public L getValue() {
			return leftValue;
		}
	}

	public final static class Right<L, R> extends Either<L, R>  {

		private final R rightValue;

		public Right(R r) {
			rightValue = r;
		}

		public boolean isRight() {
			return true;
		}

		public boolean isLeft() {
			return false;
		}

		public Right<L, R> right() {
			return this;
		}

		public Left<L, R> left() {
			return null;
		}

		public R getValue() {
			return rightValue;
		}
	}

	public void fold(Function<A> ifLeft, Function<B> ifRight) {
		if(isRight()) {
			ifRight.apply(right().getValue());
		} else {
			ifLeft.apply(left().getValue());
		}
	}

}
